package com.autobot.backend.auth.service;

import java.time.Duration;

/**
 * El inicio de sesión está bloqueado temporalmente por demasiados intentos fallidos.
 * Informa cuánto falta para que el bloqueo venza.
 */
public class CuentaBloqueadaException extends RuntimeException {

    private final long segundosRestantes;

    public CuentaBloqueadaException(Duration restante) {
        super(mensaje(segundos(restante)));
        this.segundosRestantes = segundos(restante);
    }

    /** Segundos que faltan, redondeado hacia arriba y nunca menos de 1. Sirve para el encabezado Retry-After. */
    public long getSegundosRestantes() {
        return segundosRestantes;
    }

    private static long segundos(Duration restante) {
        return Math.max(1, (restante.toMillis() + 999) / 1000);
    }

    private static String mensaje(long segundos) {
        if (segundos < 60) {
            return "Demasiados intentos fallidos. Intenta de nuevo en " + segundos
                    + (segundos == 1 ? " segundo." : " segundos.");
        }
        long minutos = (segundos + 59) / 60;
        return "Demasiados intentos fallidos. Intenta de nuevo en " + minutos
                + (minutos == 1 ? " minuto." : " minutos.");
    }
}
