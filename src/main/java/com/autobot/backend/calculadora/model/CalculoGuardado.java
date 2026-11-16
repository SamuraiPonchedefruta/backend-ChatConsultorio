package com.autobot.backend.calculadora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "calculos_guardados")
public class CalculoGuardado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "modelo_id", nullable = false)
    private Long modeloId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> supuestos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> resultado;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getModeloId() { return modeloId; }
    public void setModeloId(Long modeloId) { this.modeloId = modeloId; }
    public Map<String, Object> getSupuestos() { return supuestos; }
    public void setSupuestos(Map<String, Object> supuestos) { this.supuestos = supuestos; }
    public Map<String, Object> getResultado() { return resultado; }
    public void setResultado(Map<String, Object> resultado) { this.resultado = resultado; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
