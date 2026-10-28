package com.autobot.backend.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.model.ResetToken;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {

    Optional<ResetToken> findByTokenHash(String tokenHash);

    /**
     * Consume el token en una sola sentencia: solo lo marca si sigue sin usar y sin vencer.
     * Al ser una única sentencia atómica, si dos peticiones llegan a la vez con el mismo token solo una
     * obtiene 1; la otra obtiene 0.
     *
     * @return 1 si el token era válido y quedó consumido; 0 si estaba vencido, ya usado o no existe
     */
    @Transactional
    @Modifying
    @Query("UPDATE ResetToken t SET t.usadoEn = :ahora "
            + "WHERE t.tokenHash = :hash AND t.usadoEn IS NULL AND t.expiraEn > :ahora")
    int consumir(@Param("hash") String hash, @Param("ahora") Instant ahora);

    /** Invalida los tokens que el usuario todavía no ha usado. Se llama al emitir uno nuevo. */
    @Transactional
    @Modifying
    @Query("UPDATE ResetToken t SET t.usadoEn = :ahora "
            + "WHERE t.usuario.id = :idUsuario AND t.usadoEn IS NULL")
    int invalidarPendientes(@Param("idUsuario") Long idUsuario, @Param("ahora") Instant ahora);
}
