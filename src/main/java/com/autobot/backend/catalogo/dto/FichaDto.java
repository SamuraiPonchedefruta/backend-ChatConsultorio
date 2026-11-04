package com.autobot.backend.catalogo.dto;

import java.time.Instant;

import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;

/** Ficha pública de un modelo. Solo expone datos del catálogo, nunca la entidad. */
public record FichaDto(
        Long id,
        String marca,
        String nombre,
        String consumo,
        String mantenimiento,
        String fallasComunes,
        String repuestos,
        String queRevisar,
        Instant actualizadoEn) {

    public static FichaDto de(Modelo modelo) {
        FichaModelo ficha = modelo.getFicha();
        return new FichaDto(
                modelo.getId(),
                modelo.getMarca(),
                modelo.getNombre(),
                ficha == null ? null : ficha.getConsumo(),
                ficha == null ? null : ficha.getMantenimiento(),
                ficha == null ? null : ficha.getFallasComunes(),
                ficha == null ? null : ficha.getRepuestos(),
                ficha == null ? null : ficha.getQueRevisar(),
                ultimaActualizacion(modelo, ficha));
    }

    private static Instant ultimaActualizacion(Modelo modelo, FichaModelo ficha) {
        Instant delModelo = modelo.getActualizadoEn();
        if (ficha == null || ficha.getActualizadoEn().isBefore(delModelo)) {
            return delModelo;
        }
        return ficha.getActualizadoEn();
    }
}
