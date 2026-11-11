package com.autobot.backend.tco.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Datos que envía quien usa la calculadora de TCO.
 * Los tres supuestos finales son opcionales: si no vienen se usan los de supuesto_tco.
 */
public record CalculoRequest(
        @NotNull(message = "El modelo es obligatorio") Long idModelo,
        @NotNull(message = "El año del modelo es obligatorio") Integer anioModelo,
        @NotNull(message = "El horizonte en años es obligatorio") Integer anios,
        @NotNull(message = "El consumo en km por galón es obligatorio")
        @Positive(message = "El consumo debe ser mayor que cero") BigDecimal kmPorGalon,
        @NotNull(message = "El mantenimiento anual es obligatorio")
        @PositiveOrZero(message = "El mantenimiento anual no puede ser negativo") BigDecimal mantenimientoAnual,
        @Positive(message = "Los kilómetros por año deben ser mayores que cero")
        @DecimalMax(value = "200000", message = "Los kilómetros por año no pueden superar 200000")
        BigDecimal kmPorAnio,
        @Positive(message = "El precio del combustible debe ser mayor que cero")
        @DecimalMax(value = "100", message = "El precio del combustible no puede superar 100 USD por galón")
        BigDecimal precioCombustible,
        @PositiveOrZero(message = "El seguro anual no puede ser negativo")
        @DecimalMax(value = "100000", message = "El seguro anual no puede superar 100000 USD")
        BigDecimal seguroAnual) {
}
