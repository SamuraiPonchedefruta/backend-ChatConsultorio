package com.autobot.backend.banco.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Contenido de la nueva versión de una pregunta. */
public record PreguntaRequest(
        @NotBlank String texto,
        String imagenUrl,
        @Positive int gravedad,
        @NotNull @DecimalMin("0") BigDecimal peso,
        String explicacion,
        boolean banderaRoja,
        BigDecimal costoReparacion) {
}
