package com.autobot.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El identificador (correo o teléfono) es obligatorio")
    String identificador,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
