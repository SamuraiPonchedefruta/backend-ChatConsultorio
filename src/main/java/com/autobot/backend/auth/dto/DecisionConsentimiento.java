package com.autobot.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Una decisión sobre un tipo de consentimiento.
 *
 * No hay valores por defecto: si "aceptado" no viene en la solicitud es un error de validación,
 * nunca un sí o un no implícito. La versión es la del texto que la persona vio en pantalla.
 */
public record DecisionConsentimiento(
        @NotNull(message = "Indica explícitamente si aceptas o rechazas")
        Boolean aceptado,

        @NotBlank(message = "Indica la versión del texto legal que viste")
        @Size(max = 20, message = "La versión no puede superar los 20 caracteres")
        String version) {
}
