package com.autobot.backend.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Bloqueo por intentos fallidos contra el endpoint real POST /auth/login.
 * El límite se baja a 3 intentos para que la prueba sea rápida. El vencimiento del bloqueo (esperar
 * los minutos configurados) se comprueba en IntentosLoginTest con un reloj controlable.
 */
@SpringBootTest(properties = {"app.login.max-intentos=3", "app.login.bloqueo-minutos=15"})
@AutoConfigureMockMvc
@Testcontainers
class LoginBloqueoIntegracionTest {

    private static final String CLAVE_CORRECTA = "ClaveSegura123";
    private static final String CLAVE_INCORRECTA = "ClaveIncorrecta999";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private void registrar(String correo, String celular) throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Perez","correo":"%s","celular":"%s","password":"%s",
                                 "consentimientoDatos":{"aceptado":true,"version":"1.0"}}
                                """.formatted(correo, celular, CLAVE_CORRECTA)))
                .andExpect(status().isCreated());
    }

    private ResultActions login(String identificador, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identificador":"%s","password":"%s"}
                        """.formatted(identificador, password)));
    }

    @Test
    @DisplayName("Al llegar al límite de fallos la cuenta queda bloqueada y la respuesta informa la restricción")
    void alLlegarAlLimiteQuedaBloqueada() throws Exception {
        String correo = "ana.bloqueo@example.com";
        registrar(correo, "61238001");

        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());

        login(correo, CLAVE_INCORRECTA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(containsString("15 minutos")));
    }

    @Test
    @DisplayName("Durante el bloqueo ni la contraseña correcta permite entrar")
    void duranteElBloqueoLaContrasenaCorrectaTampocoEntra() throws Exception {
        String correo = "luis.bloqueo@example.com";
        registrar(correo, "61238002");
        for (int i = 0; i < 3; i++) {
            login(correo, CLAVE_INCORRECTA);
        }

        login(correo, CLAVE_CORRECTA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Un login exitoso reinicia el contador de fallos")
    void unExitoReiniciaElContador() throws Exception {
        String correo = "marta.reinicio@example.com";
        registrar(correo, "61238003");

        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(correo, CLAVE_CORRECTA).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());

        // Sin el reinicio, este habría sido el tercer fallo seguido y bloquearía.
        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El bloqueo es por cuenta: bloquear una no afecta a otra")
    void elBloqueoEsPorCuenta() throws Exception {
        String bloqueada = "nico.bloqueado@example.com";
        String libre = "olga.libre@example.com";
        registrar(bloqueada, "61238004");
        registrar(libre, "61238005");
        for (int i = 0; i < 3; i++) {
            login(bloqueada, CLAVE_INCORRECTA);
        }

        login(bloqueada, CLAVE_CORRECTA).andExpect(status().isTooManyRequests());
        login(libre, CLAVE_CORRECTA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Se puede iniciar sesión con el celular tal como se escribió al registrarse")
    void loginConCelularSinPrefijo() throws Exception {
        registrar("rosa.celular@example.com", "61238090");

        login("61238090", CLAVE_CORRECTA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("El correo y el celular de la misma cuenta cuentan juntos: no se esquiva el límite alternando")
    void correoYCelularCuentanJuntos() throws Exception {
        String correo = "pablo.alterna@example.com";
        registrar(correo, "61238006");

        login(correo, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login("+50761238006", CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(correo, CLAVE_INCORRECTA).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Un correo que no existe se bloquea igual: la respuesta no delata qué cuentas están registradas")
    void unCorreoInexistenteSeBloqueaIgual() throws Exception {
        String fantasma = "fantasma.inexistente@example.com";

        login(fantasma, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(fantasma, CLAVE_INCORRECTA).andExpect(status().isUnauthorized());
        login(fantasma, CLAVE_INCORRECTA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.message").value(containsString("15 minutos")));
    }
}
