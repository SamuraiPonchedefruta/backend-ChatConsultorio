package com.autobot.backend.auth.security;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUsuarioTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void adminCreaCuentaDelPersonal() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"correo":"nuevo@example.com","contrasena":"Clave12345",
                     "nombre":"Nuevo","rol":"ADMIN"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteRecibe403() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonimoRecibe401() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
