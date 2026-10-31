package com.autobot.backend.auth.security;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.model.Bitacora;
import com.autobot.backend.auth.repository.BitacoraRepository;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUsuarioTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @Autowired
    BitacoraRepository bitacoraRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        
        Rol rolAdmin = rolRepository.findByNombreRol("ADMIN")
        .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

        Usuario admin = new Usuario("Administrador", "admin@example.com", passwordEncoder.encode("Admin12345"));
        admin.agregarRol(rolAdmin);
        admin.setActivo(true);
        usuarioRepository.save(admin);
    }
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

@Test
@WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
void crearCuentaQuedaRegistradaEnBitacora() throws Exception {
    mockMvc.perform(post("/admin/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"correo":"nuevo@example.com","contrasena":"Clave12345",
                 "nombre":"Nuevo","rol":"ADMIN"}"""))
            .andExpect(status().isCreated());

    assertThat(bitacoraRepository.findByCorreoAfectadoOrderByFechaDesc("nuevo@example.com"))
            .singleElement()
            .satisfies(b -> {
                assertThat(b.getAccion()).isEqualTo(Bitacora.Accion.ALTA);
                assertThat(b.getCorreoAdmin()).isEqualTo("admin@example.com");
                assertThat(b.getFecha()).isNotNull();
            });
}
@Test
@WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
void desactivarQuedaRegistradoEnBitacora() throws Exception {
    Rol rolAdmin = rolRepository.findByNombreRol("ADMIN").orElseThrow();
    Usuario objetivo = new Usuario("Personal", "personal@example.com",
            passwordEncoder.encode("Clave12345"));
    objetivo.agregarRol(rolAdmin);
    objetivo.setActivo(true);
    objetivo = usuarioRepository.save(objetivo);

    mockMvc.perform(patch("/admin/usuarios/{id}/desactivar", objetivo.getId()))
            .andExpect(status().isOk());

    assertThat(bitacoraRepository.findByCorreoAfectadoOrderByFechaDesc("personal@example.com"))
            .singleElement()
            .satisfies(b -> {
                assertThat(b.getAccion()).isEqualTo(Bitacora.Accion.DESACTIVACION);
                assertThat(b.getCorreoAdmin()).isEqualTo("admin@example.com");
            });
}

@Test
@WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
void cambioDeRolQuedaRegistradoEnBitacora() throws Exception {
    Rol rolAdmin = rolRepository.findByNombreRol("ADMIN").orElseThrow();
    rolRepository.findByNombreRol("CLIENTE")
            .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

    Usuario objetivo = new Usuario("Personal", "personal@example.com",
            passwordEncoder.encode("Clave12345"));
    objetivo.agregarRol(rolAdmin);
    objetivo.setActivo(true);
    objetivo = usuarioRepository.save(objetivo);

    mockMvc.perform(patch("/admin/usuarios/{id}/rol", objetivo.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"rol\":\"CLIENTE\"}"))
            .andExpect(status().isOk());

    assertThat(bitacoraRepository.findByCorreoAfectadoOrderByFechaDesc("personal@example.com"))
            .singleElement()
            .satisfies(b -> {
                assertThat(b.getAccion()).isEqualTo(Bitacora.Accion.CAMBIO_ROL);
                assertThat(b.getDetalle()).contains("ADMIN").contains("CLIENTE");
            });
}

}