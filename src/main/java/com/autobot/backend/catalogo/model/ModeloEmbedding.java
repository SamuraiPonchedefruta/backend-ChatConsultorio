package com.autobot.backend.catalogo.model;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Metadatos de un embedding: de qué modelo es, de qué texto salió y con qué
 * modelo de IA se calculó.
 *
 * <p>La columna {@code embedding vector(384)} NO se mapea aquí a propósito:
 * el soporte de vectores de Hibernate depende de la versión y, con
 * {@code ddl-auto: validate}, un mapeo equivocado impediría arrancar la app.
 * Los vectores se escriben y se consultan con las consultas nativas de
 * {@link com.autobot.backend.catalogo.repository.EmbeddingRepository}.
 * Por eso la entidad es inmutable: no se persiste con {@code save()}.
 */
@Entity
@Immutable
@Table(name = "modelo_embedding")
public class ModeloEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_embedding")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modelo", nullable = false)
    private Modelo modelo;

    @Column(nullable = false, length = 60)
    private String fuente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "modelo_ia", nullable = false, length = 100)
    private String modeloIa;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ModeloEmbedding() {
        // requerido por JPA
    }

    public Long getId() {
        return id;
    }

    public Modelo getModelo() {
        return modelo;
    }

    public String getFuente() {
        return fuente;
    }

    public String getContenido() {
        return contenido;
    }

    public String getModeloIa() {
        return modeloIa;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
