package com.autobot.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * E1-05-A: las herramientas públicas (chatbot, checklist, calculadora y semáforo) se consultan sin JWT,
 * y el resto de la API sigue respondiendo 401.
 *
 * Mientras los controladores de esas herramientas no existan, una ruta pública sin token responde 404
 * (Spring la deja pasar y no encuentra handler); lo que importa aquí es que la seguridad NO la corte
 * con 401/403. Una ruta protegida, exista o no, responde 401 antes de llegar al handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RutasPublicasTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "GET {0} sin token no es bloqueada por seguridad")
    @DisplayName("Las rutas públicas aceptan GET anónimo (sin encabezado Authorization)")
    @ValueSource(strings = {
            "/chat/mensajes",
            "/checklist/items",
            "/calculadora/tco",
            "/semaforo/preguntas"
    })
    void getPublicoSinToken(String ruta) throws Exception {
        int estado = mockMvc.perform(get(ruta)).andReturn().getResponse().getStatus();

        assertThat(estado).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "POST {0} sin token no es bloqueada por seguridad")
    @DisplayName("Las rutas públicas aceptan POST anónimo (sin encabezado Authorization)")
    @ValueSource(strings = {
            "/chat/mensajes",
            "/checklist/items",
            "/calculadora/tco",
            "/semaforo/preguntas"
    })
    void postPublicoSinToken(String ruta) throws Exception {
        int estado = mockMvc.perform(post(ruta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "GET {0} sin token responde 401")
    @DisplayName("El resto de la API sigue protegida: sin token responde 401 Unauthorized")
    @ValueSource(strings = {
            "/consentimientos",
            "/autoevaluaciones",
            "/autoevaluaciones/1/resultado",
            "/modelos/1",
            "/admin/modelos",
            "/ruta-que-no-existe"
    })
    void restoDeLaApiProtegida(String ruta) throws Exception {
        mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} con token inválido en ruta protegida responde 401")
    @DisplayName("Un token alterado no abre rutas protegidas")
    @ValueSource(strings = {"/consentimientos", "/autoevaluaciones"})
    void tokenInvalidoSigueSiendo401(String ruta) throws Exception {
        mockMvc.perform(get(ruta).header("Authorization", "Bearer token.invalido.123"))
                .andExpect(status().isUnauthorized());
    }
}
