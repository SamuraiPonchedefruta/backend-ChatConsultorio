package com.autobot.backend.auth.security;

import com.autobot.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RolesAccessTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "cliente@example.com", authorities = {"ROLE_CLIENTE"})
    void clienteDebeRecibirForbiddenEnRutaAdmin() throws Exception {
        // Cliente intentando acceder a la ruta protegida de admin (espera 403 Forbidden)
        mockMvc.perform(get("/admin/recurso"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
    void adminDebeAccederARutasRestringidas() throws Exception {
        // Admin accediendo a su recurso autorizado (espera 200 OK gracias al AdminTestController)
        mockMvc.perform(get("/admin/recurso"))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioSinAutenticarDebeRetornarUnauthorized() throws Exception {
        // Usuario anónimo intentando acceder a ruta protegida (espera 401 Unauthorized)
        mockMvc.perform(get("/admin/recurso"))
                .andExpect(status().isUnauthorized());
    }
}