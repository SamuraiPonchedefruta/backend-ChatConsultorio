package com.autobot.backend.banco.model;

import java.time.Instant;
import java.util.List;

/**
 * Pregunta del banco. Su contenido (texto, gravedad, peso, etc.)
 * vive en sus versiones: ver {@link PreguntaVersion}.
 */
public record Pregunta(
        Long idPregunta,
        Integer idZona,
        String codigo,
        boolean activa,
        Instant creadoEn,
        List<PreguntaVersion> versiones) {

    public Pregunta {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
    }

    public Pregunta conVersiones(List<PreguntaVersion> nuevas) {
        return new Pregunta(idPregunta, idZona, codigo, activa, creadoEn, nuevas);
    }
}
