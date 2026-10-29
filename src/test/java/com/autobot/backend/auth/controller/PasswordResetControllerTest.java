package com.autobot.backend.auth.controller;

import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.auth.service.EmailService;
import com.autobot.backend.auth.service.ResetPasswordService;
import com.autobot.backend.chatbot.VisitanteLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PasswordResetController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = VisitanteLimitFilter.class))
@WithMockUser
public class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResetPasswordService resetPasswordService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    public void testOlvidePasswordRespuestaNeutra() throws Exception {
        when(resetPasswordService.generarToken(anyString())).thenReturn(Optional.empty());

        String requestBody = "{\"email\": \"test@example.com\"}";

        mockMvc.perform(post("/auth/olvide")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    public void testRestablecerPasswordExitoso() throws Exception {
        // Simulamos que el token es válido y pertenece al usuario con ID 1
        when(resetPasswordService.consumirToken(anyString())).thenReturn(1L);
        
        // Simulamos que el usuario existe en la base de datos
        Usuario usuarioMock = new Usuario("Test User", "test@example.com", "hashviejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.encode(anyString())).thenReturn("hashnuevo");

        String requestBody = "{\"token\": \"token-valido-123\", \"nuevaPassword\": \"NuevaClave123!\"}";

        mockMvc.perform(post("/auth/restablecer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada exitosamente"));
    }
}
