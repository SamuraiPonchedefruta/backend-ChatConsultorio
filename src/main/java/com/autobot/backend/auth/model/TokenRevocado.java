package com.autobot.backend.auth.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Token JWT cerrado con logout. Se identifica por su jti; el token en sí nunca se guarda. */
@Entity
@Table(name = "token_revocado")
public class TokenRevocado {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "fecha_revocacion", nullable = false, insertable = false, updatable = false)
    private Instant fechaRevocacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    protected TokenRevocado() {
        // requerido por JPA
    }

    public TokenRevocado(String jti, Instant fechaExpiracion) {
        this.jti = jti;
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getJti() {
        return jti;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }
}
