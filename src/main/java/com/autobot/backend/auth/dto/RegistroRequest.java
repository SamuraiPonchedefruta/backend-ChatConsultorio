package com.autobot.backend.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar una cuenta de cliente.
 *
 * El celular acepta 8 dígitos de Panamá que empiecen por 6 (61234567)
 * o el formato internacional completo (+50761234567).
 *
 * Consentimientos, sin casillas premarcadas:
 * - consentimientoDatos es obligatorio y debe ser una aceptación para poder crear la cuenta.
 * - consentimientoNotificaciones es opcional; si no se envía, no se registra ninguna decisión.
 */
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
        String correo,

        @NotBlank(message = "El celular es obligatorio")
        @Pattern(
                regexp = "^(6[0-9]{7}|\\+[1-9][0-9]{7,14})$",
                message = "El celular debe tener 8 dígitos y empezar por 6, o estar en formato internacional (+50761234567)")
        String celular,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "Debes indicar si aceptas el tratamiento de datos personales")
        @Valid
        DecisionConsentimiento consentimientoDatos,

        @Valid
        DecisionConsentimiento consentimientoNotificaciones) {

    /** Evita que la contraseña termine en los logs si alguien imprime el objeto. */
    @Override
    public String toString() {
        return "RegistroRequest[correo=" + correo + "]";
    }
}
