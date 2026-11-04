package com.autobot.backend.catalogo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Rango de precio de un modelo para un año, con la fuente y la fecha de donde salió. */
@Entity
@Table(name = "rango_precio")
public class RangoPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rango")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modelo", nullable = false)
    private Modelo modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "precio_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioMin;

    @Column(name = "precio_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioMax;

    @Column(nullable = false, length = 200)
    private String fuente;

    @Column(name = "fecha_fuente", nullable = false)
    private LocalDate fechaFuente;

    protected RangoPrecio() {
        // requerido por JPA
    }

    public RangoPrecio(Modelo modelo, Integer anio, BigDecimal precioMin, BigDecimal precioMax,
            String fuente, LocalDate fechaFuente) {
        if (precioMin.compareTo(precioMax) > 0) {
            // Falla rápido aquí; la restricción de la base de datos sigue siendo la garantía final.
            throw new IllegalArgumentException("El precio mínimo no puede ser mayor que el máximo");
        }
        this.modelo = modelo;
        this.anio = anio;
        this.precioMin = precioMin;
        this.precioMax = precioMax;
        this.fuente = fuente.trim();
        this.fechaFuente = fechaFuente;
    }

    public Long getId() {
        return id;
    }

    public Modelo getModelo() {
        return modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public BigDecimal getPrecioMin() {
        return precioMin;
    }

    public BigDecimal getPrecioMax() {
        return precioMax;
    }

    public String getFuente() {
        return fuente;
    }

    public LocalDate getFechaFuente() {
        return fechaFuente;
    }
}
