package com.autobot.backend.calculadora.repository;

import com.autobot.backend.calculadora.model.CalculoGuardado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculoGuardadoRepository extends JpaRepository<CalculoGuardado, Long> {
    List<CalculoGuardado> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}
