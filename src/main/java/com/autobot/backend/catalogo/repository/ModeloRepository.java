package com.autobot.backend.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobot.backend.catalogo.model.Modelo;

public interface ModeloRepository extends JpaRepository<Modelo, Long> {

    Optional<Modelo> findByMarcaIgnoreCaseAndNombreIgnoreCase(String marca, String nombre);
}
