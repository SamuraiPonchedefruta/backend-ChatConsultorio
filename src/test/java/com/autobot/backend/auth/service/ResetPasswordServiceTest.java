package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.ResetTokenRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.auth.service.ResetPasswordService.TokenGenerado;

/**
 * Ciclo de vida del token contra un PostgreSQL efímero con todas las migraciones. Que sea la base real
 * importa: "una sola vez" y "vencido" los hace cumplir la sentencia SQL del repositorio.
 * El reloj es manual: se adelanta a mano en lugar de esperar 30 minutos. Cada prueba se revierte sola.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ResetPasswordServiceTest {

    private static final Duration VIGENCIA = Duration.ofMinutes(30);
    private static final String CORREO = "ana.reset@example.com";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ResetTokenRepository tokens;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    private final RelojManual reloj = new RelojManual(Instant.now());
    private ResetPasswordService servicio;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        servicio = new ResetPasswordService(tokens, usuarios, VIGENCIA, reloj);
        ana = crearUsuario(CORREO);
    }

    private Usuario crearUsuario(String correo) {
        return usuarios.saveAndFlush(new Usuario("Ana Perez", correo, "hash"));
    }

    private TokenGenerado generar(String correo) {
        return servicio.generarToken(correo).orElseThrow();
    }

    @Test
    @DisplayName("Un token recién generado se puede usar y devuelve a quién pertenece")
    void elTokenGeneradoFunciona() {
        TokenGenerado generado = generar(CORREO);

        assertThat(generado.token()).hasSize(43);
        assertThat(servicio.consumirToken(generado.token())).isEqualTo(ana.getId());
    }

    @Test
    @DisplayName("El token funciona una única vez: reutilizarlo se rechaza")
    void soloFuncionaUnaVez() {
        String token = generar(CORREO).token();

        servicio.consumirToken(token);

        assertThatThrownBy(() -> servicio.consumirToken(token))
                .isInstanceOf(TokenResetInvalidoException.class);
    }

    @Test
    @DisplayName("El token expira exactamente en el tiempo definido")
    void expiraEnElTiempoDefinido() {
        TokenGenerado generado = generar(CORREO);
        assertThat(generado.expiraEn()).isEqualTo(reloj.instant().plus(VIGENCIA));

        reloj.avanzar(VIGENCIA.minusSeconds(1));

        assertThatCode(() -> servicio.consumirToken(generado.token())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Al cumplirse el tiempo el token vence y se rechaza")
    void unTokenVencidoSeRechaza() {
        String token = generar(CORREO).token();

        reloj.avanzar(VIGENCIA);

        assertThatThrownBy(() -> servicio.consumirToken(token))
                .isInstanceOf(TokenResetInvalidoException.class);
    }

    @Test
    @DisplayName("Un token vencido hace mucho tiempo también se rechaza")
    void unTokenMuyVencidoSeRechaza() {
        String token = generar(CORREO).token();

        reloj.avanzar(Duration.ofDays(2));

        assertThatThrownBy(() -> servicio.consumirToken(token))
                .isInstanceOf(TokenResetInvalidoException.class);
    }

    @Test
    @DisplayName("Un token que nunca existió se rechaza, igual que uno vacío o nulo")
    void unTokenInexistenteSeRechaza() {
        assertThatThrownBy(() -> servicio.consumirToken("este-token-nunca-se-genero"))
                .isInstanceOf(TokenResetInvalidoException.class);
        assertThatThrownBy(() -> servicio.consumirToken("")).isInstanceOf(TokenResetInvalidoException.class);
        assertThatThrownBy(() -> servicio.consumirToken("   ")).isInstanceOf(TokenResetInvalidoException.class);
        assertThatThrownBy(() -> servicio.consumirToken(null)).isInstanceOf(TokenResetInvalidoException.class);
    }

    @Test
    @DisplayName("Un token alterado se rechaza y el original sigue siendo válido")
    void unTokenAlteradoSeRechaza() {
        String token = generar(CORREO).token();
        String alterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> servicio.consumirToken(alterado))
                .isInstanceOf(TokenResetInvalidoException.class);
        assertThat(servicio.consumirToken(token)).isEqualTo(ana.getId());
    }

    @Test
    @DisplayName("Los tres motivos de rechazo dan el mismo error: no revelan si el token existió")
    void losRechazosSonIndistinguibles() {
        String usado = generar(CORREO).token();
        servicio.consumirToken(usado);
        String vencido = generar(CORREO).token();
        reloj.avanzar(VIGENCIA);

        Throwable reutilizado = catchThrowable(() -> servicio.consumirToken(usado));
        Throwable expirado = catchThrowable(() -> servicio.consumirToken(vencido));
        Throwable inexistente = catchThrowable(() -> servicio.consumirToken("no-existe"));

        assertThat(reutilizado).isInstanceOf(TokenResetInvalidoException.class);
        assertThat(expirado).isInstanceOf(TokenResetInvalidoException.class);
        assertThat(inexistente).isInstanceOf(TokenResetInvalidoException.class);
        assertThat(reutilizado.getMessage()).isEqualTo(expirado.getMessage()).isEqualTo(inexistente.getMessage());
    }

    @Test
    @DisplayName("Un token nuevo invalida el anterior que no se había usado")
    void unTokenNuevoInvalidaElAnterior() {
        String primero = generar(CORREO).token();
        String segundo = generar(CORREO).token();

        assertThat(segundo).isNotEqualTo(primero);
        assertThatThrownBy(() -> servicio.consumirToken(primero))
                .isInstanceOf(TokenResetInvalidoException.class);
        assertThat(servicio.consumirToken(segundo)).isEqualTo(ana.getId());
    }

    @Test
    @DisplayName("Los tokens de usuarios distintos son independientes")
    void losUsuariosSonIndependientes() {
        Usuario luis = crearUsuario("luis.reset@example.com");
        String deAna = generar(CORREO).token();
        String deLuis = generar("luis.reset@example.com").token();

        assertThat(servicio.consumirToken(deLuis)).isEqualTo(luis.getId());
        assertThat(servicio.consumirToken(deAna)).isEqualTo(ana.getId());
    }

    @Test
    @DisplayName("En la base solo queda el hash del token, nunca el token en claro")
    void soloSeGuardaElHash() {
        String token = generar(CORREO).token();

        String guardado = jdbc.queryForObject(
                "SELECT token_hash FROM reset_token WHERE id_usuario = ?", String.class, ana.getId());
        Integer coincidenciasConElToken = jdbc.queryForObject(
                "SELECT count(*) FROM reset_token WHERE token_hash = ?", Integer.class, token);

        assertThat(guardado).isNotEqualTo(token).matches("[0-9a-f]{64}");
        assertThat(guardado).isEqualTo(ResetPasswordService.hash(token));
        assertThat(coincidenciasConElToken).isZero();
    }

    @Test
    @DisplayName("Un correo sin cuenta no genera token ni deja filas")
    void unCorreoInexistenteNoGeneraToken() {
        assertThat(servicio.generarToken("nadie@example.com")).isEmpty();
        assertThat(servicio.generarToken("   ")).isEmpty();
        assertThat(servicio.generarToken(null)).isEmpty();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM reset_token", Integer.class)).isZero();
    }

    @Test
    @DisplayName("El correo se normaliza: mayúsculas y espacios no impiden encontrar la cuenta")
    void elCorreoSeNormaliza() {
        assertThat(servicio.generarToken("  ANA.Reset@Example.com ")).isPresent();
    }

    @Test
    @DisplayName("Una vigencia que no sea positiva impide arrancar")
    void vigenciaInvalida() {
        assertThatThrownBy(() -> new ResetPasswordService(tokens, usuarios, Duration.ZERO, reloj))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expiracion-minutos");
    }

    private static Throwable catchThrowable(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            return ex;
        }
        return null;
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class RelojManual extends Clock {

        private Instant instante;

        RelojManual(Instant inicio) {
            this.instante = inicio;
        }

        void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
