package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginDesactivadaTest extends AbstractIntegrationTest {

    private static final String LOGIN = """
            {"identificador":"%s","password":"%s"}""";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RolRepository rolRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        Rol rol = rolRepository.findByNombreRol("ADMIN")
                .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

        Usuario activo = new Usuario("Activo", "activo@example.com", passwordEncoder.encode("Clave12345"));
        activo.agregarRol(rol);
        activo.setActivo(true);
        usuarioRepository.save(activo);

        Usuario inactivo = new Usuario("Inactivo", "inactivo@example.com", passwordEncoder.encode("Clave12345"));
        inactivo.agregarRol(rol);
        inactivo.setActivo(false);
        usuarioRepository.save(inactivo);
    }

    @Test
    void cuentaDesactivadaNoRecibeTokenAunqueLaContrasenaSeaCorrecta() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN.formatted("inactivo@example.com", "Clave12345")))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain("token")
                .doesNotContain("eyJ");
    }

    @Test
    void cuentaActivaSigueIniciandoSesion() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN.formatted("activo@example.com", "Clave12345")))
                .andExpect(status().isOk());
    }

    @Test
    void cuentaDesactivadaConContrasenaErroneaRecibeElMismoRechazoQueCualquierCredencialInvalida() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN.formatted("inactivo@example.com", "ClaveIncorrecta1")))
                .andExpect(status().isUnauthorized());
    }
}
