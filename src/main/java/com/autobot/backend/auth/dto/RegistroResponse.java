package com.autobot.backend.auth.dto;

import java.time.Instant;

/** Datos públicos de la cuenta recién creada. Nunca incluye la contraseña ni su hash. */
public record RegistroResponse(
        Long id,
        String nombre,
        String correo,
        String celular,
        String rol,
        Instant fechaRegistro) {
}
