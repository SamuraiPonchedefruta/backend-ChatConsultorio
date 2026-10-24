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

@Entity
@Table(name = "telefono_usuario")
public class TelefonoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefono")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 16)
    private String numero;

    @Column(name = "es_principal", nullable = false)
    private boolean principal;

    @Column(name = "verificado_en")
    private Instant verificadoEn;

    protected TelefonoUsuario() {
        // requerido por JPA
    }

    // Solo se crea a través de Usuario.agregarTelefono(...)
    TelefonoUsuario(Usuario usuario, String numero, boolean principal) {
        this.usuario = usuario;
        this.numero = numero;
        this.principal = principal;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public Instant getVerificadoEn() {
        return verificadoEn;
    }

    public boolean isVerificado() {
        return verificadoEn != null;
    }
}
