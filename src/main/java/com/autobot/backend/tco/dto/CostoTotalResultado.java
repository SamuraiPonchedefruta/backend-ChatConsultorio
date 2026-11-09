package com.autobot.backend.tco.dto;

import java.math.BigDecimal;

/** Desglose del costo total de propiedad en los cinco rubros, en USD y para todo el horizonte. */
public record CostoTotalResultado(
        int anios,
        BigDecimal compra,
        BigDecimal combustible,
        BigDecimal mantenimiento,
        BigDecimal seguro,
        BigDecimal gastosAnuales,
        BigDecimal total) {

    /** Arma el resultado y calcula el total como la suma exacta de los cinco rubros. */
    public static CostoTotalResultado de(int anios, BigDecimal compra, BigDecimal combustible,
            BigDecimal mantenimiento, BigDecimal seguro, BigDecimal gastosAnuales) {
        BigDecimal total = compra.add(combustible).add(mantenimiento).add(seguro).add(gastosAnuales);
        return new CostoTotalResultado(anios, compra, combustible, mantenimiento, seguro,
                gastosAnuales, total);
    }
}
