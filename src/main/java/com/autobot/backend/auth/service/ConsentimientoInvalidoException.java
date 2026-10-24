package com.autobot.backend.auth.service;

/**
 * La decisión de consentimiento no se puede registrar: falta la decisión explícita,
 * la versión del texto no existe o el consentimiento obligatorio no fue aceptado.
 */
public class ConsentimientoInvalidoException extends RuntimeException {

    public ConsentimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
