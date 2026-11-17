package com.autobot.backend.banco.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Una versión concreta del contenido de una {@link Pregunta}. */
public record PreguntaVersion(
        Long idPreguntaVersion,
        int numero,
        String texto,
        String imagenUrl,
        int gravedad,
        BigDecimal peso,
        String explicacion,
        boolean banderaRoja,
        BigDecimal costoReparacion,
        boolean vigente,
        Instant creadoEn) {
}
