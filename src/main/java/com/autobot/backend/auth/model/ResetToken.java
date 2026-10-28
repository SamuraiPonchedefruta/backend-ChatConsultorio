package com.autobot.backend.auth.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token de restablecimiento de contraseña de un solo uso. Se identifica por el hash SHA-256 del token;
 * el token en claro nunca se guarda.
 */
@Entity
@Table(name = "reset_token")
public class ResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reset_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false, updatable = false)
    private Instant expiraEn;

    /** Instante en que se consumió o se invalidó; null mientras siga disponible. */
    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected ResetToken() {
        // requerido por JPA
    }

    public ResetToken(Usuario usuario, String tokenHash, Instant expiraEn, Instant creadoEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getUsadoEn() {
        return usadoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
