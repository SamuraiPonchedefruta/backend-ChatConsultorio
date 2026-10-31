package com.autobot.backend.auth.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bitacora")
public class Bitacora {

    public enum Accion {
        ALTA,
        DESACTIVACION,
        CAMBIO_ROL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Long id;

    @Column(name = "fecha", nullable = false, updatable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 30)
    private Accion accion;

    @Column(name = "correo_admin", nullable = false, length = 255)
    private String correoAdmin;

    @Column(name = "correo_afectado", nullable = false, length = 255)
    private String correoAfectado;

    @Column(name = "detalle", length = 500)
    private String detalle;

    protected Bitacora() {
        // requerido por JPA
    }

    public Bitacora(Accion accion, String correoAdmin, String correoAfectado, String detalle) {
        this.fecha = Instant.now();
        this.accion = accion;
        this.correoAdmin = correoAdmin;
        this.correoAfectado = correoAfectado;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Instant getFecha() { return fecha; }
    public Accion getAccion() { return accion; }
    public String getCorreoAdmin() { return correoAdmin; }
    public String getCorreoAfectado() { return correoAfectado; }
    public String getDetalle() { return detalle; }
}
