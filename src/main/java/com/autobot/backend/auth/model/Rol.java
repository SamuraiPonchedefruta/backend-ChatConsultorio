package com.autobot.backend.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 30)
    private String nombreRol;

    protected Rol() {
        // requerido por JPA
    }

    public Rol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public Integer getId() {
        return id;
    }

    public String getNombreRol() {
        return nombreRol;
    }
}
