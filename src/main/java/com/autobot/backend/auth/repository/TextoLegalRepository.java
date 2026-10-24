package com.autobot.backend.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobot.backend.auth.model.TextoLegal;

@Repository
public interface TextoLegalRepository extends JpaRepository<TextoLegal, Integer> {

    Optional<TextoLegal> findByTipoAndVersion(String tipo, String version);

    /** Versión más reciente de un tipo de texto (la que se le muestra a quien va a aceptar). */
    Optional<TextoLegal> findFirstByTipoOrderByVigenteDesdeDesc(String tipo);
}
