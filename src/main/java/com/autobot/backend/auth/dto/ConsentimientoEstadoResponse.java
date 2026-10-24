package com.autobot.backend.auth.dto;

import java.time.Instant;

/**
 * Estado actual de un tipo de consentimiento para un usuario.
 *
 * estado: ACEPTADO, RECHAZADO o SIN_DECISION (todavía no eligió; la casilla debe verse sin marcar).
 * versionAceptada y fechaDecision son null cuando no hay decisión.
 * versionVigente es la versión más reciente del texto, para que el cliente sepa qué mostrar.
 */
public record ConsentimientoEstadoResponse(
        String tipo,
        String estado,
        String versionAceptada,
        Instant fechaDecision,
        String versionVigente) {
}
