package com.autobot.backend.auth.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Los tipos de consentimiento que se gestionan por separado. Cada uno tiene su propio texto legal,
 * su propio historial de decisiones y ninguno depende de otro.
 */
public enum TipoConsentimiento {

    DATOS("datos", "TRATAMIENTO_DATOS"),
    NOTIFICACIONES("notificaciones", "NOTIFICACIONES");

    private final String ruta;
    private final String tipoTexto;

    TipoConsentimiento(String ruta, String tipoTexto) {
        this.ruta = ruta;
        this.tipoTexto = tipoTexto;
    }

    /** Nombre público del tipo: el que viaja en la URL y en las respuestas. */
    public String getRuta() {
        return ruta;
    }

    /** Valor de la columna tipo de texto_legal que corresponde a este consentimiento. */
    public String getTipoTexto() {
        return tipoTexto;
    }

    public static Optional<TipoConsentimiento> desdeRuta(String ruta) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.ruta.equalsIgnoreCase(ruta))
                .findFirst();
    }
}
