package com.autobot.backend.catalogo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.service.BitacoraService;
import com.autobot.backend.catalogo.dto.ModeloRequest;
import com.autobot.backend.catalogo.dto.ModeloResponse;
import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.common.config.CacheConfig;

@Service
public class ModeloService {

    private final ModeloRepository modelos;
    private final BitacoraService bitacora;

    public ModeloService(ModeloRepository modelos, BitacoraService bitacora) {
        this.modelos = modelos;
        this.bitacora = bitacora;
    }

    @Transactional
    public ModeloResponse crear(ModeloRequest req, String correoAdminActual) {
        String marca = req.marca().trim();
        String nombre = req.nombre().trim();
        validarNoDuplicado(marca, nombre, null);

        Modelo modelo = new Modelo(marca, nombre);
        modelo.asignarFicha(nuevaFicha(req));
        Modelo guardado = modelos.save(modelo);

        bitacora.registrarAltaModelo(correoAdminActual, guardado.getId(), descripcion(guardado));
        return ModeloResponse.de(guardado);
    }

    /** Al editar, se invalida la ficha cacheada de ese modelo (cuando la transacción confirma). */
    @CacheEvict(cacheNames = CacheConfig.FICHAS, key = "#id")
    @Transactional
    public ModeloResponse editar(Long id, ModeloRequest req, String correoAdminActual) {
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

        bitacora.registrarEdicionModelo(correoAdminActual, modelo.getId(), descripcion(modelo));
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

    private String descripcion(Modelo modelo) {
        return modelo.getMarca() + " " + modelo.getNombre();
    }
}
