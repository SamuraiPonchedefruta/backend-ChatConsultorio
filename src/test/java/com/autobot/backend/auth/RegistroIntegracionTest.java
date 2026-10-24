package com.autobot.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

/**
 * Prueba de punta a punta: endpoint real, Spring Security y PostgreSQL efímero con las migraciones de Flyway.
 * Cada prueba usa correo y celular propios para no depender del orden de ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RegistroIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static String json(String nombre, String correo, String celular, String password) {
        return """
                {"nombre":"%s","correo":"%s","celular":"%s","password":"%s"}
                """.formatted(nombre, correo, celular, password);
    }

    @Test
    @DisplayName("Registra al cliente, guarda el hash BCrypt y no devuelve datos sensibles")
    void registraClienteConHashBcrypt() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Perez", "Ana.Registro@Example.com", "61230001", "ClaveSegura123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.correo").value("ana.registro@example.com"))
                .andExpect(jsonPath("$.celular").value("+50761230001"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE correo = ?", String.class, "ana.registro@example.com");
        assertThat(hash).startsWith("$2").isNotEqualTo("ClaveSegura123");

        String rol = jdbc.queryForObject("""
                SELECT r.nombre_rol
                FROM usuario u
                JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
                JOIN rol r ON r.id_rol = ur.id_rol
                WHERE u.correo = ?
                """, String.class, "ana.registro@example.com");
        assertThat(rol).isEqualTo("CLIENTE");

        String celular = jdbc.queryForObject("""
                SELECT t.numero
                FROM telefono_usuario t
                JOIN usuario u ON u.id_usuario = t.id_usuario
                WHERE u.correo = ?
                """, String.class, "ana.registro@example.com");
        assertThat(celular).isEqualTo("+50761230001");
    }

    @Test
    @DisplayName("Un correo repetido responde 409 con mensaje claro")
    void rechazaCorreoRepetido() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Luis Gomez", "luis.dup@example.com", "61230002", "ClaveSegura123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Otro Luis", "LUIS.DUP@example.com", "61230003", "ClaveSegura123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("correo")))
                .andExpect(jsonPath("$.details[0]").value(containsString("correo")))
                .andExpect(jsonPath("$.path").value("/auth/registro"));
    }

    @Test
    @DisplayName("Un celular repetido responde 409 con mensaje claro")
    void rechazaCelularRepetido() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Marta Diaz", "marta.cel@example.com", "61230004", "ClaveSegura123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Otra Marta", "marta.otra@example.com", "+50761230004", "ClaveSegura123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("celular")))
                .andExpect(jsonPath("$.details[0]").value(containsString("celular")));
    }

    @Test
    @DisplayName("Datos inválidos responden 400 con el detalle por campo")
    void rechazaDatosInvalidos() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", "esto-no-es-un-correo", "123", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("El health check sigue público y el resto de rutas exige autenticación")
    void rutasPublicasYProtegidas() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cualquier-ruta-protegida"))
                .andExpect(status().isUnauthorized());
    }
}
