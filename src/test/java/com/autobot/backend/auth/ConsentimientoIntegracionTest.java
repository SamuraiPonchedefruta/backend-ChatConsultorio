package com.autobot.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Endpoints de consentimiento del perfil, contra un PostgreSQL efímero con todas las migraciones.
 * Todavía no hay login: el usuario autenticado se simula con user(correo), que es la convención
 * (Principal.getName() es el correo) que debe respetar el login cuando exista.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ConsentimientoIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** Crea la cuenta por el endpoint real, aceptando solo el tratamiento de datos. */
    private void registrar(String correo, String celular) throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Perez","correo":"%s","celular":"%s","password":"ClaveSegura123",
                                 "consentimientoDatos":{"aceptado":true,"version":"1.0"}}
                                """.formatted(correo, celular)))
                .andExpect(status().isCreated());
    }

    private static String decision(boolean aceptado) {
        return "{\"aceptado\":%s,\"version\":\"1.0\"}".formatted(aceptado);
    }

    private void actualizar(String correo, String tipo, boolean aceptado) throws Exception {
        mockMvc.perform(put("/consentimientos/" + tipo)
                        .with(user(correo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision(aceptado)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tras registrarse, datos figura aceptado y notificaciones viene SIN_DECISION (casilla sin marcar)")
    void notificacionesNoVieneMarcadaPorDefecto() throws Exception {
        registrar("ines.estado@example.com", "61231001");

        mockMvc.perform(get("/consentimientos").with(user("ines.estado@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("datos"))
                .andExpect(jsonPath("$[0].estado").value("ACEPTADO"))
                .andExpect(jsonPath("$[0].versionAceptada").value("1.0"))
                .andExpect(jsonPath("$[0].fechaDecision").isNotEmpty())
                .andExpect(jsonPath("$[1].tipo").value("notificaciones"))
                .andExpect(jsonPath("$[1].estado").value("SIN_DECISION"))
                .andExpect(jsonPath("$[1].versionAceptada").doesNotExist())
                .andExpect(jsonPath("$[1].versionVigente").value("1.0"));
    }

    @Test
    @DisplayName("Cada tipo opera de forma independiente: cambiar uno no altera el otro")
    void cadaTipoEsIndependiente() throws Exception {
        String correo = "jorge.indep@example.com";
        registrar(correo, "61231002");

        actualizar(correo, "notificaciones", true);
        actualizar(correo, "datos", false);

        mockMvc.perform(get("/consentimientos").with(user(correo)))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[1].estado").value("ACEPTADO"));

        actualizar(correo, "notificaciones", false);

        mockMvc.perform(get("/consentimientos").with(user(correo)))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[1].estado").value("RECHAZADO"));
    }

    @Test
    @DisplayName("Las actualizaciones del perfil quedan vinculadas al usuario y suman al historial del registro")
    void lasActualizacionesQuedanVinculadasAlUsuario() throws Exception {
        String correo = "laura.historial@example.com";
        registrar(correo, "61231003");

        actualizar(correo, "notificaciones", true);
        actualizar(correo, "datos", false);

        List<String> historial = jdbc.queryForList("""
                SELECT t.tipo || ':' || t.version || ':' || c.aceptado
                FROM consentimiento c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                JOIN texto_legal t ON t.id_texto_legal = c.id_texto_legal
                WHERE u.correo = ? AND c.fecha_decision IS NOT NULL
                ORDER BY c.id_consentimiento
                """, String.class, correo);

        assertThat(historial).containsExactly(
                "TRATAMIENTO_DATOS:1.0:true",
                "NOTIFICACIONES:1.0:true",
                "TRATAMIENTO_DATOS:1.0:false");
    }

    @Test
    @DisplayName("Repetir la misma decisión no agrega filas al historial")
    void repetirLaMismaDecisionNoDuplica() throws Exception {
        String correo = "mario.repite@example.com";
        registrar(correo, "61231004");

        actualizar(correo, "notificaciones", true);
        actualizar(correo, "notificaciones", true);

        Integer filas = jdbc.queryForObject("""
                SELECT count(*)
                FROM consentimiento c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                WHERE u.correo = ?
                """, Integer.class, correo);
        assertThat(filas).isEqualTo(2);
    }

    @Test
    @DisplayName("Sin la decisión explícita (aceptado ausente) responde 400")
    void exigeDecisionExplicita() throws Exception {
        String correo = "nico.explicito@example.com";
        registrar(correo, "61231005");

        mockMvc.perform(put("/consentimientos/notificaciones")
                        .with(user(correo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"1.0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("aceptado")));
    }

    @Test
    @DisplayName("Una versión inexistente del texto responde 400 con el motivo")
    void rechazaVersionInexistente() throws Exception {
        String correo = "olga.version@example.com";
        registrar(correo, "61231006");

        mockMvc.perform(put("/consentimientos/datos")
                        .with(user(correo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptado\":true,\"version\":\"9.9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("9.9")));
    }

    @Test
    @DisplayName("Un tipo de consentimiento desconocido responde 404")
    void tipoDesconocidoResponde404() throws Exception {
        String correo = "pablo.tipo@example.com";
        registrar(correo, "61231007");

        mockMvc.perform(put("/consentimientos/marketing")
                        .with(user(correo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision(true)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin autenticación los endpoints de consentimiento responden 401")
    void exigeAutenticacion() throws Exception {
        mockMvc.perform(get("/consentimientos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/consentimientos/datos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision(true)))
                .andExpect(status().isUnauthorized());
    }
}
