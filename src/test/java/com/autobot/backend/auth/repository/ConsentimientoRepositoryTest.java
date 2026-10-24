package com.autobot.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.autobot.backend.auth.model.Consentimiento;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.Usuario;

/**
 * Corre contra un PostgreSQL efímero con todas las migraciones de Flyway aplicadas.
 * Cada prueba se revierte al terminar, así que no dependen del orden.
 * Las pruebas de restricciones terminan en la sentencia que falla: tras un error,
 * PostgreSQL deja abortada la transacción de la prueba.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ConsentimientoRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private TextoLegalRepository textos;

    @Autowired
    private ConsentimientoRepository consentimientos;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario crearUsuario(String correo) {
        return usuarios.saveAndFlush(new Usuario("Ana Perez", correo, "hash"));
    }

    private TextoLegal crearTexto(String tipo, String version, Instant vigenteDesde) {
        return textos.saveAndFlush(new TextoLegal(tipo, version, "Contenido de prueba", vigenteDesde));
    }

    private TextoLegal crearTexto(String tipo, String version) {
        return crearTexto(tipo, version, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("Flyway aplica la migración V3 y crea las dos tablas")
    void migracionV3Aplicada() {
        Boolean aplicada = jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '3'", Boolean.class);
        Integer tablas = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('texto_legal', 'consentimiento')
                """, Integer.class);

        assertThat(aplicada).isTrue();
        assertThat(tablas).isEqualTo(2);
    }

    @Test
    @DisplayName("Cada decisión guarda su instante exacto y la versión del texto legal")
    void guardaDecisionConTimestampYVersion() {
        Usuario usuario = crearUsuario("ana.consent@example.com");
        TextoLegal texto = crearTexto("TERMINOS", "1.0");

        Instant antes = Instant.now();
        Consentimiento guardado = consentimientos.saveAndFlush(new Consentimiento(usuario, texto, true));
        Instant despues = Instant.now();
        em.clear();

        Consentimiento leido = consentimientos.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getFechaDecision()).isBetween(antes.minusSeconds(1), despues.plusSeconds(1));
        assertThat(leido.getTextoLegal().getId()).isEqualTo(texto.getId());
        assertThat(leido.getTextoLegal().getVersion()).isEqualTo("1.0");
        assertThat(leido.isAceptado()).isTrue();
    }

    @Test
    @DisplayName("Cambiar de opinión agrega una decisión nueva y conserva el historial")
    void conservaElHistorialDeDecisiones() {
        Usuario usuario = crearUsuario("luis.historial@example.com");
        TextoLegal texto = crearTexto("TERMINOS", "1.0");

        consentimientos.saveAndFlush(new Consentimiento(usuario, texto, true));
        consentimientos.saveAndFlush(new Consentimiento(usuario, texto, false));

        List<Consentimiento> historial =
                consentimientos.findByUsuarioIdOrderByFechaDecisionDescIdDesc(usuario.getId());
        assertThat(historial).extracting(Consentimiento::isAceptado).containsExactly(false, true);

        Consentimiento vigente = consentimientos
                .findFirstByUsuarioIdAndTextoLegalIdOrderByFechaDecisionDescIdDesc(usuario.getId(), texto.getId())
                .orElseThrow();
        assertThat(vigente.isAceptado()).isFalse();
    }

    @Test
    @DisplayName("La versión más reciente de un tipo de texto es la de vigencia más nueva")
    void encuentraLaVersionMasReciente() {
        crearTexto("PRIVACIDAD", "1.0", Instant.parse("2026-01-01T00:00:00Z"));
        crearTexto("PRIVACIDAD", "2.0", Instant.parse("2026-06-01T00:00:00Z"));

        TextoLegal reciente = textos.findFirstByTipoOrderByVigenteDesdeDesc("PRIVACIDAD").orElseThrow();

        assertThat(reciente.getVersion()).isEqualTo("2.0");
        assertThat(textos.findByTipoAndVersion("PRIVACIDAD", "1.0")).isPresent();
    }

    @Test
    @DisplayName("No se puede guardar una decisión sin versión de texto legal")
    void rechazaConsentimientoSinVersionDeTexto() {
        Usuario usuario = crearUsuario("marta.sinversion@example.com");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO consentimiento (id_usuario, id_texto_legal, aceptado) VALUES (?, NULL, TRUE)",
                usuario.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("No se puede repetir una versión del mismo tipo de texto")
    void rechazaVersionDuplicada() {
        crearTexto("TERMINOS", "1.0");

        assertThatThrownBy(() -> crearTexto("TERMINOS", "1.0"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Un consentimiento guardado no se puede modificar")
    void noPermiteModificarUnConsentimiento() {
        Usuario usuario = crearUsuario("pedro.inmutable@example.com");
        TextoLegal texto = crearTexto("TERMINOS", "1.0");
        Consentimiento guardado = consentimientos.saveAndFlush(new Consentimiento(usuario, texto, true));

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE consentimiento SET aceptado = FALSE WHERE id_consentimiento = ?", guardado.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutable");
    }

    @Test
    @DisplayName("Un texto legal publicado no se puede modificar")
    void noPermiteModificarUnTextoLegal() {
        TextoLegal texto = crearTexto("TERMINOS", "1.0");

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE texto_legal SET contenido = 'otra redaccion' WHERE id_texto_legal = ?", texto.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutable");
    }
}
