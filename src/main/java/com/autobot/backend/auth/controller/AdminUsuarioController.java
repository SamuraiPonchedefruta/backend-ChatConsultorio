package com.autobot.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.auth.dto.CambioRolRequest;
import com.autobot.backend.auth.dto.CuentaPersonalRequest;
import com.autobot.backend.auth.dto.UsuarioResponse;
import com.autobot.backend.auth.service.AdminUsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService servicio;

    public AdminUsuarioController(AdminUsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CuentaPersonalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(servicio.crear(req)));
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id,
                                      @Valid @RequestBody CambioRolRequest req,
                                      Authentication auth) {
        return UsuarioResponse.de(servicio.cambiarRol(id, req.rol(), auth.getName()));
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@PathVariable Long id, Authentication auth) {
        return UsuarioResponse.de(servicio.desactivar(id, auth.getName()));
    }
}
