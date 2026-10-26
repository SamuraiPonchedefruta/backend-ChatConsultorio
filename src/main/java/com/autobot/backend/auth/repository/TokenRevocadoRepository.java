package com.autobot.backend.auth.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.model.TokenRevocado;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    /** Revoca el token. Si ya estaba revocado no hace nada, incluso con dos peticiones simultáneas. */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO token_revocado (jti, fecha_expiracion) VALUES (:jti, :expiracion) "
            + "ON CONFLICT (jti) DO NOTHING", nativeQuery = true)
    int registrar(@Param("jti") String jti, @Param("expiracion") Instant expiracion);

    /** Borra los revocados que ya vencieron: ese token ya no sirve por su propia expiración. */
    @Transactional
    @Modifying
    @Query("delete from TokenRevocado t where t.fechaExpiracion < :ahora")
    int purgarVencidos(@Param("ahora") Instant ahora);
}
