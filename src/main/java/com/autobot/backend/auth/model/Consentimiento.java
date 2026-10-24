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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Una decisión de consentimiento (aceptar o rechazar/revocar) sobre una versión concreta de un texto legal.
 * Es un registro histórico: no se modifica, y cambiar de opinión es guardar una decisión nueva.
 */
@Entity
@Table(name = "consentimiento")
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_texto_legal", nullable = false, updatable = false)
    private TextoLegal textoLegal;

    @Column(nullable = false, updatable = false)
    private boolean aceptado;

    @Column(name = "fecha_decision", nullable = false, updatable = false)
    private Instant fechaDecision;

    protected Consentimiento() {
        // requerido por JPA
    }

    public Consentimiento(Usuario usuario, TextoLegal textoLegal, boolean aceptado) {
        this.usuario = usuario;
        this.textoLegal = textoLegal;
        this.aceptado = aceptado;
    }

    /** Marca el instante exacto de la decisión en el momento de guardarla. */
    @PrePersist
    void alCrear() {
        if (fechaDecision == null) {
            fechaDecision = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TextoLegal getTextoLegal() {
        return textoLegal;
    }

    public boolean isAceptado() {
        return aceptado;
    }

    public Instant getFechaDecision() {
        return fechaDecision;
    }
}
