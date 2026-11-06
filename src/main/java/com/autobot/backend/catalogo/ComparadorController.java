package com.autobot.backend.catalogo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/modelos")
public class ComparadorController {

    private final ComparadorService comparadorService;

    public ComparadorController(ComparadorService comparadorService) {
        this.comparadorService = comparadorService;
    }

    // Ejemplos: GET /modelos/comparar?ids=1,2,3
    //           GET /modelos/comparar?ids=1,2&anio=2018
    @GetMapping("/comparar")
    public ComparadorService.Comparacion comparar(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) Integer anio) {
        return comparadorService.comparar(ids, anio);
    }
}
