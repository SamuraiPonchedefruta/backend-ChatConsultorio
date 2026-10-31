package com.autobot.backend.auth.security;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtRealAuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        
        Rol rolAdmin = rolRepository.findByNombreRol("ADMIN")
        .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

Rol rolCliente = rolRepository.findByNombreRol("CLIENTE")
        .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

        admin = new Usuario("Administrador", "admin@example.com", passwordEncoder.encode("Admin12345"));
        admin.agregarRol(rolAdmin);
        admin.setActivo(true);
        usuarioRepository.save(admin);

        cliente = new Usuario("Cliente", "cliente@example.com", passwordEncoder.encode("Client12345"));
        cliente.agregarRol(rolCliente);
        cliente.setActivo(true);
        usuarioRepository.save(cliente);
    }

   @Test
void adminConTokenRealCreaCuentaExitosamente() throws Exception {
    String token = jwtService.generarToken("admin@example.com", List.of("ADMIN"));

    mockMvc.perform(post("/admin/usuarios")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "correo": "personal.nuevo@example.com",
                    "contrasena": "ClaveSegura123",
                    "nombre": "Personal Nuevo",
                    "rol": "ADMIN"
                }
            """))
            .andExpect(status().isCreated());
}

@Test
void clienteConTokenRealRecibeForbidden() throws Exception {
    String tokenCliente = jwtService.generarToken("cliente@example.com", List.of("CLIENTE"));

    mockMvc.perform(post("/admin/usuarios")
            .header("Authorization", "Bearer " + tokenCliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden());
}
}