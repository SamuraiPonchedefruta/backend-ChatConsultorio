package com.autobot.backend.common.error;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Estructura uniforme de error para toda la API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {

    public static ErrorResponse of(HttpStatusCode status, String message, String path, List<String> details) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new ErrorResponse(Instant.now(), status.value(), reason, message, path, details);
    }
}
