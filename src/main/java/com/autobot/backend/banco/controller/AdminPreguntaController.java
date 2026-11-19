package com.autobot.backend.banco.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.banco.dto.PreguntaRequest;
import com.autobot.backend.banco.model.PreguntaVersion;
import com.autobot.backend.banco.service.PreguntaService;

import jakarta.validation.Valid;

/** /admin/** ya exige ROL ADMIN en SecurityConfig. Principal.getName() es el correo. */
@RestController
@RequestMapping("/admin/preguntas")
public class AdminPreguntaController {

    private final PreguntaService servicio;

    public AdminPreguntaController(PreguntaService servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/{idPregunta}")
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaVersion editar(@PathVariable long idPregunta,
                                  @Valid @RequestBody PreguntaRequest req,
                                  Principal principal) {
        return servicio.editar(idPregunta, req, principal.getName());
    }
}
