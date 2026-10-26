package com.autobot.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del JWT (prefijo app.jwt).
 *
 * El secreto viene de la variable de entorno JWT_SECRET y no tiene valor por defecto en ningún perfil:
 * si falta o es demasiado corto, la aplicación no arranca.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("60") long expirationMinutes,
        @DefaultValue("autobot-backend") String issuer) {

    /** Evita que el secreto termine en los logs si alguien imprime el objeto. */
    @Override
    public String toString() {
        return "JwtProperties[expirationMinutes=" + expirationMinutes + ", issuer=" + issuer + "]";
    }
}
