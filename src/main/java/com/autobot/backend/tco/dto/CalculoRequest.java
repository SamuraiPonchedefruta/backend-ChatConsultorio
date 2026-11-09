package com.autobot.backend.tco.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos que envía quien usa la calculadora de TCO. */
public record CalculoRequest(
        @NotNull Long idModelo,
        @NotNull Integer anioModelo,
        @NotNull Integer anios,
        @NotNull @Positive BigDecimal kmPorGalon,
        @NotNull @PositiveOrZero BigDecimal mantenimientoAnual) {
}
