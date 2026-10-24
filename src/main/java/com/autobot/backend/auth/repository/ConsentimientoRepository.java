package com.autobot.backend.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobot.backend.auth.model.Consentimiento;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    /** Historial completo de un usuario, de la decisión más reciente a la más antigua. */
    List<Consentimiento> findByUsuarioIdOrderByFechaDecisionDescIdDesc(Long idUsuario);

    /** Decisión vigente de un usuario sobre una versión de texto: la más reciente. */
    Optional<Consentimiento> findFirstByUsuarioIdAndTextoLegalIdOrderByFechaDecisionDescIdDesc(
            Long idUsuario, Integer idTextoLegal);

    /**
     * Decisión vigente de un usuario sobre un tipo de texto (por ejemplo TRATAMIENTO_DATOS),
     * sin importar la versión: la más reciente.
     */
    Optional<Consentimiento> findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(
            Long idUsuario, String tipoTexto);
}
