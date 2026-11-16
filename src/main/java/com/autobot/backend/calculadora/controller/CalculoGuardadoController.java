package com.autobot.backend.calculadora.controller;

import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.calculadora.model.CalculoGuardado;
import com.autobot.backend.calculadora.repository.CalculoGuardadoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calculos-guardados")
public class CalculoGuardadoController {

    public record GuardarRequest(
            @NotNull Long modeloId,
            @NotEmpty Map<String, Object> supuestos,
            @NotEmpty Map<String, Object> resultado) {}

    private final CalculoGuardadoRepository repo;
    private final UsuarioRepository usuarios;

    public CalculoGuardadoController(CalculoGuardadoRepository repo, UsuarioRepository usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalculoGuardado guardar(@Valid @RequestBody GuardarRequest req, Authentication auth) {
        CalculoGuardado c = new CalculoGuardado();
        c.setUsuarioId(usuarioId(auth));
        c.setModeloId(req.modeloId());
        c.setSupuestos(req.supuestos());
        c.setResultado(req.resultado());
        return repo.save(c);
    }

    @GetMapping
    public List<CalculoGuardado> mios(Authentication auth) {
        return repo.findByUsuarioIdOrderByCreadoEnDesc(usuarioId(auth));
    }

    private Long usuarioId(Authentication auth) {
        return usuarios.findByCorreo(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }
}
