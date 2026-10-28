package com.autobot.backend.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreoRecuperacion(String destinatario, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de contraseña - Autobot");
        mensaje.setText("Para restablecer tu contraseña, haz clic en el siguiente enlace:\n" +
                "http://localhost:8080/auth/restablecer?token=" + token);
        mailSender.send(mensaje);
    }
}
