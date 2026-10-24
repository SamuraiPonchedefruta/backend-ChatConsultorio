package com.autobot.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Prueba de punta a punta: endpoint real, Spring Security y PostgreSQL efímero con las migraciones de Flyway
 * (incluida la V4, que carga la versión 1.0 de los textos legales).
 * Cada prueba usa correo y celular propios para no depender del orden de ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RegistroIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String DATOS_OK = "\"consentimientoDatos\":{\"aceptado\":true,\"version\":\"1.0\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static String json(String nombre, String correo, String celular, String password, String consentimientos) {
        String extra = consentimientos.isEmpty() ? "" : "," + consentimientos;
        return """
                {"nombre":"%s","correo":"%s","celular":"%s","password":"%s"%s}
                """.formatted(nombre, correo, celular, password, extra);
    }

    private static String json(String nombre, String correo, String celular, String password) {
        return json(nombre, correo, celular, password, DATOS_OK);
    }

    private int cuentas(String correo) {
        return jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo = ?", Integer.class, correo);
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
    @DisplayName("El registro deja el consentimiento de datos vinculado a la cuenta, con versión y fecha, y ninguno de notificaciones")
    void registroGuardaElConsentimientoDeDatos() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Raul Vega", "raul.datos@example.com", "61230010", "ClaveSegura123")))
                .andExpect(status().isCreated());

        List<String> decisiones = jdbc.queryForList("""
                SELECT t.tipo || ':' || t.version || ':' || c.aceptado
                FROM consentimiento c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                JOIN texto_legal t ON t.id_texto_legal = c.id_texto_legal
                WHERE u.correo = ? AND c.fecha_decision IS NOT NULL
                """, String.class, "raul.datos@example.com");

        assertThat(decisiones).containsExactly("TRATAMIENTO_DATOS:1.0:true");
    }

    @Test
    @DisplayName("Si se envía la decisión de notificaciones, queda guardada de forma independiente (aquí, un rechazo)")
    void registroGuardaLaDecisionDeNotificaciones() throws Exception {
        String consentimientos = DATOS_OK + ",\"consentimientoNotificaciones\":{\"aceptado\":false,\"version\":\"1.0\"}";

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Sara Lopez", "sara.notif@example.com", "61230011", "ClaveSegura123",
                                consentimientos)))
                .andExpect(status().isCreated());

        List<String> decisiones = jdbc.queryForList("""
                SELECT t.tipo || ':' || c.aceptado
                FROM consentimiento c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                JOIN texto_legal t ON t.id_texto_legal = c.id_texto_legal
                WHERE u.correo = ?
                ORDER BY c.id_consentimiento
                """, String.class, "sara.notif@example.com");

        assertThat(decisiones).containsExactly("TRATAMIENTO_DATOS:true", "NOTIFICACIONES:false");
    }

    @Test
    @DisplayName("Sin el consentimiento de datos en la solicitud (casilla sin marcar) responde 400 y no crea la cuenta")
    void rechazaSinConsentimientoDeDatos() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Omar Diaz", "omar.sin@example.com", "61230012", "ClaveSegura123", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("consentimientoDatos")));

        assertThat(cuentas("omar.sin@example.com")).isZero();
    }

    @Test
    @DisplayName("Rechazar el tratamiento de datos responde 400 con el motivo y no crea la cuenta")
    void rechazaCuandoNoAceptaLosDatos() throws Exception {
        String consentimientos = "\"consentimientoDatos\":{\"aceptado\":false,\"version\":\"1.0\"}";

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nora Cruz", "nora.no@example.com", "61230013", "ClaveSegura123",
                                consentimientos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("datos")));

        assertThat(cuentas("nora.no@example.com")).isZero();
    }

    @Test
    @DisplayName("Una versión inexistente del texto legal responde 400 y no crea la cuenta")
    void rechazaVersionInexistenteDelTexto() throws Exception {
        String consentimientos = "\"consentimientoDatos\":{\"aceptado\":true,\"version\":\"9.9\"}";

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ivan Rios", "ivan.version@example.com", "61230014", "ClaveSegura123",
                                consentimientos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("9.9")));

        assertThat(cuentas("ivan.version@example.com")).isZero();
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
