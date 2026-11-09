package com.autobot.backend.tco.dto;

import java.time.LocalDate;

/** Respuesta pública de la calculadora: el desglose y el contexto para interpretarlo. */
public record CalculoResponse(
        CostoTotalResultado desglose,
        String alcance,
        String limitaciones,
        LocalDate fechaDatos) {
}
