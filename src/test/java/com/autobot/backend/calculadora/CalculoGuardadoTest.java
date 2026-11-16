package com.autobot.backend.calculadora;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.testcontainers.utility.DockerImageName;

/**
 * Guardado y consulta de cálculos por cliente, contra un PostgreSQL efímero con todas las migraciones.
 * El usuario autenticado se simula con user(correo), igual que en los tests de consentimiento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CalculoGuardadoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** Crea la cuenta por el endpoint real. */
    private void registrar(String correo, String celular) throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Perez","correo":"%s","celular":"%s","password":"ClaveSegura123",
                                 "consentimientoDatos":{"aceptado":true,"version":"1.0"}}
                                """.formatted(correo, celular)))
                .andExpect(status().isCreated());
    }

    /** AJUSTAR: si la tabla modelo exige más columnas NOT NULL, agrégalas aquí. */
    private Long crearModelo() {
        return jdbc.queryForObject(
                "INSERT INTO modelo (marca, nombre) VALUES (?, ?) RETURNING id_modelo",
                Long.class, "Toyota", "Yaris " + System.nanoTime());
    }

    private static String cuerpo(Long modeloId) {
        return """
                {"modeloId":%d,
                 "supuestos":{"anios":5,"kmAnual":15000},
                 "resultado":{"total":12345.67}}
                """.formatted(modeloId);
    }

    private void guardar(String correo, Long modeloId) throws Exception {
        mockMvc.perform(post("/calculos-guardados")
                        .with(user(correo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(modeloId)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Un cliente guarda un cálculo: 201, con supuestos y resultado, sin exponer el id de usuario")
    void clienteGuardaUnCalculo() throws Exception {
        registrar("ana.guarda@example.com", "61232001");
        Long modeloId = crearModelo();

        mockMvc.perform(post("/calculos-guardados")
                        .with(user("ana.guarda@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(modeloId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.modeloId").value(modeloId))
                .andExpect(jsonPath("$.supuestos.anios").value(5))
                .andExpect(jsonPath("$.resultado.total").value(12345.67))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    @DisplayName("Un cliente consulta solo sus propios cálculos")
    void clienteSoloVeLosSuyos() throws Exception {
        registrar("beto.dueno@example.com", "61232002");
        registrar("carla.ajena@example.com", "61232003");
        Long modeloId = crearModelo();

        guardar("beto.dueno@example.com", modeloId);

        mockMvc.perform(get("/calculos-guardados").with(user("beto.dueno@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].modeloId").value(modeloId));

        mockMvc.perform(get("/calculos-guardados").with(user("carla.ajena@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Sin supuestos o sin resultado responde 400")
    void exigeSupuestosYResultado() throws Exception {
        registrar("dani.valida@example.com", "61232004");
        Long modeloId = crearModelo();

        mockMvc.perform(post("/calculos-guardados")
                        .with(user("dani.valida@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modeloId\":%d}".formatted(modeloId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin autenticación los endpoints responden 401")
    void exigeAutenticacion() throws Exception {
        mockMvc.perform(get("/calculos-guardados"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/calculos-guardados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(1L)))
                .andExpect(status().isUnauthorized());
    }
}
