package com.autobot.backend.auth.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "correo_verificado_en")
    private Instant correoVerificadoEn;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @ManyToMany
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol"))
    private Set<Rol> roles = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TelefonoUsuario> telefonos = new ArrayList<>();

    protected Usuario() {
        // requerido por JPA
    }

    public Usuario(String nombre, String correo, String passwordHash) {
        this.nombre = nombre;
        this.correo = normalizarCorreo(correo);
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }

    /** El teléfono debe venir en formato internacional, por ejemplo +50761234567. */
    public void agregarTelefono(String numero, boolean principal) {
        telefonos.add(new TelefonoUsuario(this, numero.trim(), principal));
    }

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCorreoVerificadoEn() {
        return correoVerificadoEn;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public Set<Rol> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public List<TelefonoUsuario> getTelefonos() {
        return Collections.unmodifiableList(telefonos);
    }
}
