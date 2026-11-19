package com.autobot.backend.banco.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.service.BitacoraService;
import com.autobot.backend.banco.dto.PreguntaRequest;
import com.autobot.backend.banco.model.Pregunta;
import com.autobot.backend.banco.model.PreguntaVersion;
import com.autobot.backend.banco.repository.PreguntaRepository;

@Service
public class PreguntaService {

    private final PreguntaRepository repositorio;
    private final BitacoraService bitacora;

    public PreguntaService(PreguntaRepository repositorio, BitacoraService bitacora) {
        this.repositorio = repositorio;
        this.bitacora = bitacora;
    }

    /** Editar = crear una versión nueva vigente. El contenido de las anteriores no se modifica. */
    @Transactional
    public PreguntaVersion editar(long idPregunta, PreguntaRequest req, String correoAdmin) {
        Pregunta pregunta = repositorio.buscarPorId(idPregunta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pregunta no encontrada: " + idPregunta));

        PreguntaVersion nueva = repositorio.agregarVersion(idPregunta, new PreguntaVersion(
                null, 0, req.texto(), req.imagenUrl(), req.gravedad(), req.peso(),
                req.explicacion(), req.banderaRoja(), req.costoReparacion(), true, null));

        bitacora.registrarEdicionPregunta(correoAdmin, idPregunta,
                "Nueva versión v" + nueva.numero() + " de " + pregunta.codigo());
        return nueva;
    }
}
