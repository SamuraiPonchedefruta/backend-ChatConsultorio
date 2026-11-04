package com.autobot.backend.catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobot.backend.catalogo.model.RangoPrecio;

public interface RangoPrecioRepository extends JpaRepository<RangoPrecio, Long> {

    /** Rangos de un modelo en un año, del más reciente al más antiguo. */
    List<RangoPrecio> findByModeloIdAndAnioOrderByFechaFuenteDesc(Long idModelo, Integer anio);
}
