package com.autobot.backend.catalogo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.dto.ModeloRequest;
import com.autobot.backend.catalogo.dto.ModeloResponse;
import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;

@Service
public class ModeloService {

    private final ModeloRepository modelos;

    public ModeloService(ModeloRepository modelos) {
        this.modelos = modelos;
    }

    @Transactional
    public ModeloResponse crear(ModeloRequest req) {
        String marca = req.marca().trim();
        String nombre = req.nombre().trim();
        validarNoDuplicado(marca, nombre, null);

        Modelo modelo = new Modelo(marca, nombre);
        modelo.asignarFicha(nuevaFicha(req));
        return ModeloResponse.de(modelos.save(modelo));
    }

    @Transactional
    public ModeloResponse editar(Long id, ModeloRequest req) {
        Modelo modelo = modelos.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));

        String marca = req.marca().trim();
        String nombre = req.nombre().trim();
        validarNoDuplicado(marca, nombre, id);

        modelo.actualizarDatos(marca, nombre);
        if (modelo.getFicha() == null) {
            modelo.asignarFicha(nuevaFicha(req));
        } else {
            modelo.getFicha().actualizar(req.consumo(), req.mantenimiento(),
                    req.fallasComunes(), req.repuestos(), req.queRevisar());
        }
        return ModeloResponse.de(modelo);
    }

    /** Un modelo con la misma marca y nombre solo es válido si es el mismo que se edita. */
    private void validarNoDuplicado(String marca, String nombre, Long idPropio) {
        modelos.findByMarcaIgnoreCaseAndNombreIgnoreCase(marca, nombre)
                .filter(existente -> !existente.getId().equals(idPropio))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe un modelo con esa marca y nombre");
                });
    }

    private FichaModelo nuevaFicha(ModeloRequest req) {
        return new FichaModelo(req.consumo(), req.mantenimiento(),
                req.fallasComunes(), req.repuestos(), req.queRevisar());
    }
}
