package com.autobot.backend.catalogo.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.catalogo.dto.ModeloRequest;
import com.autobot.backend.catalogo.dto.ModeloResponse;
import com.autobot.backend.catalogo.service.ModeloService;

import jakarta.validation.Valid;

/** El acceso se restringe al rol ADMIN con la regla /admin/** de SecurityConfig. */
@RestController
@RequestMapping("/admin/modelos")
public class AdminModeloController {

    private final ModeloService servicio;

    public AdminModeloController(ModeloService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ModeloResponse crear(@Valid @RequestBody ModeloRequest req) {
        return servicio.crear(req);
    }

    @PutMapping("/{id}")
    public ModeloResponse editar(@PathVariable Long id, @Valid @RequestBody ModeloRequest req) {
        return servicio.editar(id, req);
    }
}
