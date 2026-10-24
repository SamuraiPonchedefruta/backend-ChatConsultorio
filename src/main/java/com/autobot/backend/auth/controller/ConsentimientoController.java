package com.autobot.backend.auth.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.dto.ConsentimientoEstadoResponse;
import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.model.TipoConsentimiento;
import com.autobot.backend.auth.service.ConsentimientoService;

import jakarta.validation.Valid;

/**
 * Consentimientos del usuario autenticado. Nunca recibe un id de usuario: siempre actúa sobre quien
 * está autenticado.
 *
 * Convención con el login: Principal.getName() debe ser el correo del usuario.
 */
@RestController
@RequestMapping("/consentimientos")
public class ConsentimientoController {

    private final ConsentimientoService consentimientoService;

    public ConsentimientoController(ConsentimientoService consentimientoService) {
        this.consentimientoService = consentimientoService;
    }

    /** Estado de cada tipo; los que no tienen decisión vienen como SIN_DECISION. */
    @GetMapping
    public List<ConsentimientoEstadoResponse> estado(Principal principal) {
        return consentimientoService.estadoDe(principal.getName());
    }

    /** Acepta o rechaza un tipo de consentimiento (datos o notificaciones). */
    @PutMapping("/{tipo}")
    public ConsentimientoEstadoResponse actualizar(
            @PathVariable("tipo") String tipo,
            @Valid @RequestBody DecisionConsentimiento decision,
            Principal principal) {
        TipoConsentimiento tipoConsentimiento = TipoConsentimiento.desdeRuta(tipo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return consentimientoService.actualizar(principal.getName(), tipoConsentimiento, decision);
    }
}
