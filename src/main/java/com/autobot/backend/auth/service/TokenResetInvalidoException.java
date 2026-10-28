package com.autobot.backend.auth.service;

/**
 * El token de restablecimiento no se puede usar. Es la misma excepción y el mismo mensaje tanto si el
 * token venció, como si ya se usó o nunca existió, para no dar pistas a quien esté probando tokens.
 */
public class TokenResetInvalidoException extends RuntimeException {

    public TokenResetInvalidoException() {
        super("El enlace de recuperación no es válido o ya venció.");
    }
}
