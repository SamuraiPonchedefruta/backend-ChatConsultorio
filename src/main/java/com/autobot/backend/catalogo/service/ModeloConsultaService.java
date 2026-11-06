package com.autobot.backend.catalogo.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.dto.FichaDto;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.common.config.CacheConfig;

/** Consultas de solo lectura del catálogo, usadas por los endpoints públicos. */
@Service
public class ModeloConsultaService {

    private final ModeloRepository modelos;

    public ModeloConsultaService(ModeloRepository modelos) {
        this.modelos = modelos;
    }

    /** Se cachea el DTO (inmutable), nunca la entidad. Un 404 lanza excepción y por eso no se cachea. */
    @Cacheable(cacheNames = CacheConfig.FICHAS, key = "#id")
    @Transactional(readOnly = true)
    public FichaDto obtenerFicha(Long id) {
        return modelos.findById(id)
                .map(FichaDto::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));
    }
}
