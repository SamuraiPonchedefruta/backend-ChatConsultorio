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
        CAMBIO_ROL,
        MODELO_ALTA,
        MODELO_EDICION
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

    /** Solo las acciones sobre cuentas lo llenan. */
    @Column(name = "correo_afectado", length = 255)
    private String correoAfectado;

    /** Solo las acciones sobre modelos del catálogo lo llenan. */
    @Column(name = "id_modelo")
    private Long idModelo;

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

    public static Bitacora deModelo(Accion accion, String correoAdmin, Long idModelo, String detalle) {
        Bitacora registro = new Bitacora(accion, correoAdmin, null, detalle);
        registro.idModelo = idModelo;
        return registro;
    }

    public Long getId() { return id; }
    public Instant getFecha() { return fecha; }
    public Accion getAccion() { return accion; }
    public String getCorreoAdmin() { return correoAdmin; }
    public String getCorreoAfectado() { return correoAfectado; }
    public Long getIdModelo() { return idModelo; }
    public String getDetalle() { return detalle; }
}
