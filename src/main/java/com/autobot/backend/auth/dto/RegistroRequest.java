package com.autobot.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistroRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato de correo no es válido")
    String correo,

    @NotBlank(message = "El celular es obligatorio")
    String celular,

    @NotBlank(message = "La contraseña es obligatoria")
    String password,

    @NotNull(message = "El consentimiento de datos es obligatorio")
    DecisionConsentimiento consentimientoDatos,

    DecisionConsentimiento consentimientoNotificaciones
) {}
