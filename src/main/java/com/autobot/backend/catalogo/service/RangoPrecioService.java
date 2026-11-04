package com.autobot.backend.catalogo.service;

import java.time.Year;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.dto.RangoPrecioRequest;
import com.autobot.backend.catalogo.dto.RangoPrecioResponse;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

@Service
public class RangoPrecioService {

    /** Primer año admitido; coincide con ck_rango_precio_anio de la migración V12. */
    private static final int ANIO_MINIMO = 1900;

    private final ModeloRepository modelos;
    private final RangoPrecioRepository rangos;

    public RangoPrecioService(ModeloRepository modelos, RangoPrecioRepository rangos) {
        this.modelos = modelos;
        this.rangos = rangos;
    }

    @Transactional
    public RangoPrecioResponse guardar(Long idModelo, RangoPrecioRequest req) {
        Modelo modelo = modelos.findById(idModelo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));

        validarAnio(req.anio());
        if (req.precioMin().compareTo(req.precioMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El precio mínimo no puede ser mayor que el precio máximo");
        }

        RangoPrecio guardado = rangos.save(new RangoPrecio(modelo, req.anio(),
                req.precioMin(), req.precioMax(), req.fuente(), req.fechaFuente()));
        return RangoPrecioResponse.de(guardado);
    }

    /** Se admite hasta el año siguiente, porque los modelos del próximo año se venden antes de que empiece. */
    private void validarAnio(int anio) {
        int anioMaximo = Year.now().getValue() + 1;
        if (anio < ANIO_MINIMO || anio > anioMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año debe estar entre " + ANIO_MINIMO + " y " + anioMaximo);
        }
    }
}
