package com.autobot.backend.catalogo.dto;

import java.time.Instant;

import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;

public record ModeloResponse(
        Long id,
        String marca,
        String nombre,
        String consumo,
        String mantenimiento,
        String fallasComunes,
        String repuestos,
        String queRevisar,
        Instant actualizadoEn) {

    public static ModeloResponse de(Modelo modelo) {
        FichaModelo ficha = modelo.getFicha();
        return new ModeloResponse(
                modelo.getId(),
                modelo.getMarca(),
                modelo.getNombre(),
                ficha == null ? null : ficha.getConsumo(),
                ficha == null ? null : ficha.getMantenimiento(),
                ficha == null ? null : ficha.getFallasComunes(),
                ficha == null ? null : ficha.getRepuestos(),
                ficha == null ? null : ficha.getQueRevisar(),
                modelo.getActualizadoEn());
    }
}
