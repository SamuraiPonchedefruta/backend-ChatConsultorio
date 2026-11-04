package com.autobot.backend.catalogo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.catalogo.dto.RangoPrecioRequest;
import com.autobot.backend.catalogo.dto.RangoPrecioResponse;
import com.autobot.backend.catalogo.service.RangoPrecioService;

import jakarta.validation.Valid;

/** El acceso se restringe al rol ADMIN con la regla /admin/** de SecurityConfig. */
@RestController
@RequestMapping("/admin/modelos/{idModelo}/rangos-precio")
public class RangoPrecioController {

    private final RangoPrecioService servicio;

    public RangoPrecioController(RangoPrecioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RangoPrecioResponse> guardar(@PathVariable Long idModelo,
            @Valid @RequestBody RangoPrecioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.guardar(idModelo, req));
    }
}
