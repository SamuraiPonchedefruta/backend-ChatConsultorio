package com.autobot.backend.tco.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Supuesto por defecto del TCO (km por año, combustible, seguro, gastos), con la fecha de sus datos. */
@Entity
@Table(name = "supuesto_tco")
public class SupuestoTco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_supuesto")
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String clave;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 30)
    private String unidad;

    @Column(nullable = false, length = 200)
    private String fuente;

    @Column(name = "fecha_fuente", nullable = false)
    private LocalDate fechaFuente;

    protected SupuestoTco() {
        // requerido por JPA
    }

    public SupuestoTco(String clave, String descripcion, BigDecimal valor, String unidad,
            String fuente, LocalDate fechaFuente) {
        validarValor(valor);
        this.clave = clave.trim();
        this.descripcion = descripcion.trim();
        this.valor = valor;
        this.unidad = unidad.trim();
        this.fuente = fuente.trim();
        this.fechaFuente = fechaFuente;
    }

    /** Actualiza el valor y la procedencia de los datos; la fecha siempre viaja con el valor. */
    public void actualizar(BigDecimal nuevoValor, String nuevaFuente, LocalDate nuevaFechaFuente) {
        validarValor(nuevoValor);
        this.valor = nuevoValor;
        this.fuente = nuevaFuente.trim();
        this.fechaFuente = nuevaFechaFuente;
    }

    private static void validarValor(BigDecimal valor) {
        if (valor.signum() < 0) {
            // Falla rápido aquí; la restricción de la base de datos sigue siendo la garantía final.
            throw new IllegalArgumentException("El valor del supuesto no puede ser negativo");
        }
    }

    public Long getId() {
        return id;
    }

    public String getClave() {
        return clave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getUnidad() {
        return unidad;
    }

    public String getFuente() {
        return fuente;
    }

    public LocalDate getFechaFuente() {
        return fechaFuente;
    }
}
