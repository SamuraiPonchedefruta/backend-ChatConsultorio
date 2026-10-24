package com.autobot.backend.auth.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Una versión publicada de un texto legal (términos, privacidad, etc.).
 * Una vez guardada no se edita: un cambio de redacción es una versión nueva.
 */
@Entity
@Table(name = "texto_legal")
public class TextoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_texto_legal")
    private Integer id;

    /** Tipo en MAYÚSCULAS_CON_GUION_BAJO, por ejemplo TERMINOS o PRIVACIDAD. */
    @Column(nullable = false, length = 50, updatable = false)
    private String tipo;

    @Column(nullable = false, length = 20, updatable = false)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String contenido;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private Instant vigenteDesde;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected TextoLegal() {
        // requerido por JPA
    }

    public TextoLegal(String tipo, String version, String contenido, Instant vigenteDesde) {
        this.tipo = tipo;
        this.version = version;
        this.contenido = contenido;
        this.vigenteDesde = vigenteDesde;
    }

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getVersion() {
        return version;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getVigenteDesde() {
        return vigenteDesde;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
