package com.autobot.backend.catalogo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Datos del rango de precio (en USD). La coherencia entre campos (mínimo contra máximo y
 * rango del año) se valida en RangoPrecioService.
 */
public record RangoPrecioRequest(
        @NotNull(message = "El año es obligatorio")
        Integer anio,

        @NotNull(message = "El precio mínimo es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio mínimo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio mínimo admite hasta 10 enteros y 2 decimales")
        BigDecimal precioMin,

        @NotNull(message = "El precio máximo es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio máximo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio máximo admite hasta 10 enteros y 2 decimales")
        BigDecimal precioMax,

        @NotBlank(message = "La fuente es obligatoria")
        @Size(max = 200, message = "La fuente admite máximo 200 caracteres")
        String fuente,

        @NotNull(message = "La fecha de la fuente es obligatoria")
        @PastOrPresent(message = "La fecha de la fuente no puede ser futura")
        LocalDate fechaFuente) {
}
