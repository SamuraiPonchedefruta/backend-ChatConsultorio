package com.autobot.backend.catalogo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ficha_modelo")
public class FichaModelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ficha")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modelo", nullable = false, unique = true)
    private Modelo modelo;

    @Column(columnDefinition = "TEXT")
    private String consumo;

    @Column(columnDefinition = "TEXT")
    private String mantenimiento;

    @Column(name = "fallas_comunes", columnDefinition = "TEXT")
    private String fallasComunes;

    @Column(columnDefinition = "TEXT")
    private String repuestos;

    @Column(name = "que_revisar", columnDefinition = "TEXT")
    private String queRevisar;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected FichaModelo() {
        // requerido por JPA
    }

    public FichaModelo(String consumo, String mantenimiento, String fallasComunes,
            String repuestos, String queRevisar) {
        actualizar(consumo, mantenimiento, fallasComunes, repuestos, queRevisar);
    }

    @PrePersist
    @PreUpdate
    void marcarActualizacion() {
        actualizadoEn = Instant.now();
    }

    public void actualizar(String consumo, String mantenimiento, String fallasComunes,
            String repuestos, String queRevisar) {
        this.consumo = consumo;
        this.mantenimiento = mantenimiento;
        this.fallasComunes = fallasComunes;
        this.repuestos = repuestos;
        this.queRevisar = queRevisar;
    }

    /** Solo lo usa Modelo.asignarFicha para mantener la relación bidireccional. */
    void asignarModelo(Modelo modelo) {
        this.modelo = modelo;
    }

    public Long getId() {
        return id;
    }

    public Modelo getModelo() {
        return modelo;
    }

    public String getConsumo() {
        return consumo;
    }

    public String getMantenimiento() {
        return mantenimiento;
    }

    public String getFallasComunes() {
        return fallasComunes;
    }

    public String getRepuestos() {
        return repuestos;
    }

    public String getQueRevisar() {
        return queRevisar;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
