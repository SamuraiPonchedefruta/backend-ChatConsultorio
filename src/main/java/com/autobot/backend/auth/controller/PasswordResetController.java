package com.autobot.backend.auth.controller;

import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.auth.service.EmailService;
import com.autobot.backend.auth.service.ResetPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/olvide")
    public ResponseEntity<?> solicitarRestauracion(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email != null && !email.isBlank()) {
            Optional<ResetPasswordService.TokenGenerado> tokenOpt = resetPasswordService.generarToken(email);
            
            if (tokenOpt.isPresent()) {
                emailService.enviarCorreoRecuperacion(email, tokenOpt.get().token());
            }
        }

        return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, se han enviado las instrucciones."));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecerPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String nuevaPassword = request.get("nuevaPassword");

        try {
            Long usuarioId = resetPasswordService.consumirToken(token);

            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Usamos setPasswordHash que corresponde al campo de la entidad
            usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token inválido o expirado"));
        }
    }
}