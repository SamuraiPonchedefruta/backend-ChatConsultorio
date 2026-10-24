package com.autobot.backend.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.autobot.backend.auth.service.CuentaDuplicadaException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Cualquier error que nadie más atrapó: 500 con mensaje genérico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Error no controlado en {}", pathOf(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders(), request, List.of());
    }

    /** Correo o celular ya registrados: 409 con mensaje claro y el detalle por campo. */
    @ExceptionHandler(CuentaDuplicadaException.class)
    public ResponseEntity<Object> handleCuentaDuplicada(CuentaDuplicadaException ex, WebRequest request) {
        List<String> details = ex.getCampos().stream()
                .map(campo -> campo + ": ya está registrado")
                .toList();
        ErrorResponse body = ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), pathOf(request), details);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /** Errores de validación: 400 con el detalle por campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(status, headers, request, details);
    }

    /** Errores propios de Spring MVC (404, 405, 400, 415...): mismo formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return build(statusCode, headers, request, List.of());
    }

    private ResponseEntity<Object> build(
            HttpStatusCode status, HttpHeaders headers, WebRequest request, List<String> details) {
        ErrorResponse body = ErrorResponse.of(status, messageFor(status), pathOf(request), details);
        return new ResponseEntity<>(body, headers, status);
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "La solicitud no es válida";
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "Método HTTP no permitido para este recurso";
            case 415 -> "Tipo de contenido no soportado";
            default -> status.is5xxServerError()
                    ? "Ocurrió un error interno. Intenta de nuevo más tarde."
                    : "No se pudo procesar la solicitud";
        };
    }

    private static String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";
    }
}
