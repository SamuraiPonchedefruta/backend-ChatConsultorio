package com.autobot.backend.catalogo.model;

import java.time.Instant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "modelo")
public class Modelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modelo")
    private Long id;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToOne(mappedBy = "modelo", cascade = CascadeType.ALL, orphanRemoval = true)
    private FichaModelo ficha;

    protected Modelo() {
        // requerido por JPA
    }

    public Modelo(String marca, String nombre) {
        this.marca = marca.trim();
        this.nombre = nombre.trim();
    }

    /** Se ejecuta en cada insert y en cada update de la fila. */
    @PrePersist
    @PreUpdate
    void marcarActualizacion() {
        actualizadoEn = Instant.now();
    }

    public void actualizarDatos(String marca, String nombre) {
        this.marca = marca.trim();
        this.nombre = nombre.trim();
    }

    /** Mantiene sincronizados los dos lados de la relación. */
    public void asignarFicha(FichaModelo nuevaFicha) {
        if (nuevaFicha != null) {
            nuevaFicha.asignarModelo(this);
        }
        this.ficha = nuevaFicha;
    }

    public Long getId() {
        return id;
    }

    public String getMarca() {
        return marca;
    }

    public String getNombre() {
        return nombre;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public FichaModelo getFicha() {
        return ficha;
    }
}
