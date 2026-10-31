package com.autobot.backend.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobot.backend.auth.model.Bitacora;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    List<Bitacora> findByCorreoAfectadoOrderByFechaDesc(String correoAfectado);
}
