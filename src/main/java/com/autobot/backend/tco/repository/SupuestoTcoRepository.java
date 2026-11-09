package com.autobot.backend.tco.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobot.backend.tco.model.SupuestoTco;

public interface SupuestoTcoRepository extends JpaRepository<SupuestoTco, Long> {

    /** Busca un supuesto por su clave estable (por ejemplo KM_POR_ANIO). */
    Optional<SupuestoTco> findByClave(String clave);

    /** Todos los supuestos ordenados por clave, para mostrarlos o editarlos. */
    List<SupuestoTco> findAllByOrderByClaveAsc();
}
