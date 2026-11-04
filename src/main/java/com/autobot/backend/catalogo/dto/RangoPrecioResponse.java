package com.autobot.backend.catalogo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autobot.backend.catalogo.model.RangoPrecio;

public record RangoPrecioResponse(
        Long id,
        Long idModelo,
        Integer anio,
        BigDecimal precioMin,
        BigDecimal precioMax,
        String fuente,
        LocalDate fechaFuente) {

    public static RangoPrecioResponse de(RangoPrecio rango) {
        return new RangoPrecioResponse(
                rango.getId(),
                rango.getModelo().getId(),
                rango.getAnio(),
                rango.getPrecioMin(),
                rango.getPrecioMax(),
                rango.getFuente(),
                rango.getFechaFuente());
    }
}
