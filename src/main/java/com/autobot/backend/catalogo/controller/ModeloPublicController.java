package com.autobot.backend.catalogo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.catalogo.dto.FichaDto;
import com.autobot.backend.catalogo.service.ModeloConsultaService;

/** Consulta pública: se abre en SecurityConfig solo para GET /modelos/{id}. */
@RestController
@RequestMapping("/modelos")
public class ModeloPublicController {

    private final ModeloConsultaService consulta;

    public ModeloPublicController(ModeloConsultaService consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/{id}")
    public FichaDto obtener(@PathVariable Long id) {
        return consulta.obtenerFicha(id);
    }
}
