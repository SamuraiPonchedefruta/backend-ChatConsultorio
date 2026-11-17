package com.autobot.backend.banco.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.banco.model.Pregunta;
import com.autobot.backend.banco.model.PreguntaVersion;
import com.autobot.backend.banco.model.Zona;

@Repository
public class PreguntaRepository {

    private static final RowMapper<Zona> ZONA_MAPPER = (rs, i) -> new Zona(
            rs.getInt("id_zona"),
            rs.getString("codigo"),
            rs.getString("nombre"),
            rs.getInt("orden"));

    private static final RowMapper<Pregunta> PREGUNTA_MAPPER = (rs, i) -> new Pregunta(
            rs.getLong("id_pregunta"),
            rs.getInt("id_zona"),
            rs.getString("codigo"),
            rs.getBoolean("activa"),
            rs.getTimestamp("creado_en").toInstant(),
            List.of());

    private static final RowMapper<PreguntaVersion> VERSION_MAPPER = (rs, i) -> mapVersion(rs);

    private final JdbcTemplate jdbc;

    public PreguntaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- Zonas ----------

    public List<Zona> listarZonas() {
        return jdbc.query("SELECT id_zona, codigo, nombre, orden FROM zona ORDER BY orden", ZONA_MAPPER);
    }

    // ---------- Preguntas ----------

    /** Inserta la pregunta junto con todas las versiones que traiga. */
    @Transactional
    public Pregunta guardar(Pregunta pregunta) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pregunta (id_zona, codigo, activa) VALUES (?, ?, ?)",
                    new String[] {"id_pregunta"});
            ps.setInt(1, pregunta.idZona());
            ps.setString(2, pregunta.codigo());
            ps.setBoolean(3, pregunta.activa());
            return ps;
        }, kh);
        long idPregunta = kh.getKey().longValue();

        for (PreguntaVersion v : pregunta.versiones()) {
            insertarVersion(idPregunta, v);
        }
        return buscarPorId(idPregunta).orElseThrow();
    }

    /**
     * Agrega una nueva versión a una pregunta existente. El número se asigna
     * automáticamente (max + 1) y, si la nueva es vigente, desactiva la anterior.
     */
    @Transactional
    public PreguntaVersion agregarVersion(long idPregunta, PreguntaVersion nueva) {
        Integer siguiente = jdbc.queryForObject(
                "SELECT COALESCE(MAX(numero), 0) + 1 FROM pregunta_version WHERE id_pregunta = ?",
                Integer.class, idPregunta);
        if (nueva.vigente()) {
            jdbc.update("UPDATE pregunta_version SET vigente = FALSE WHERE id_pregunta = ?", idPregunta);
        }
        PreguntaVersion conNumero = new PreguntaVersion(null, siguiente, nueva.texto(), nueva.imagenUrl(),
                nueva.gravedad(), nueva.peso(), nueva.explicacion(), nueva.banderaRoja(),
                nueva.costoReparacion(), nueva.vigente(), null);
        return insertarVersion(idPregunta, conNumero);
    }

    public Optional<Pregunta> buscarPorId(long idPregunta) {
        return jdbc.query("SELECT id_pregunta, id_zona, codigo, activa, creado_en FROM pregunta WHERE id_pregunta = ?",
                PREGUNTA_MAPPER, idPregunta).stream().findFirst().map(this::conVersiones);
    }

    public Optional<Pregunta> buscarPorCodigo(String codigo) {
        return jdbc.query("SELECT id_pregunta, id_zona, codigo, activa, creado_en FROM pregunta WHERE codigo = ?",
                PREGUNTA_MAPPER, codigo).stream().findFirst().map(this::conVersiones);
    }

    public List<Pregunta> listarActivasPorZona(int idZona) {
        return jdbc.query("SELECT id_pregunta, id_zona, codigo, activa, creado_en FROM pregunta "
                        + "WHERE id_zona = ? AND activa = TRUE ORDER BY codigo",
                PREGUNTA_MAPPER, idZona).stream().map(this::conVersiones).toList();
    }

    public List<PreguntaVersion> listarVersiones(long idPregunta) {
        return jdbc.query("SELECT * FROM pregunta_version WHERE id_pregunta = ? ORDER BY numero",
                VERSION_MAPPER, idPregunta);
    }

    // ---------- Internos ----------

    private Pregunta conVersiones(Pregunta p) {
        return p.conVersiones(new ArrayList<>(listarVersiones(p.idPregunta())));
    }

    private PreguntaVersion insertarVersion(long idPregunta, PreguntaVersion v) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pregunta_version (id_pregunta, numero, texto, imagen_url, gravedad, peso, "
                            + "explicacion, bandera_roja, costo_reparacion, vigente) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] {"id_pregunta_version"});
            ps.setLong(1, idPregunta);
            ps.setInt(2, v.numero());
            ps.setString(3, v.texto());
            ps.setString(4, v.imagenUrl());
            ps.setInt(5, v.gravedad());
            ps.setBigDecimal(6, v.peso());
            ps.setString(7, v.explicacion());
            ps.setBoolean(8, v.banderaRoja());
            if (v.costoReparacion() == null) {
                ps.setNull(9, Types.NUMERIC);
            } else {
                ps.setBigDecimal(9, v.costoReparacion());
            }
            ps.setBoolean(10, v.vigente());
            return ps;
        }, kh);
        return jdbc.queryForObject("SELECT * FROM pregunta_version WHERE id_pregunta_version = ?",
                VERSION_MAPPER, kh.getKey().longValue());
    }

    private static PreguntaVersion mapVersion(ResultSet rs) throws SQLException {
        return new PreguntaVersion(
                rs.getLong("id_pregunta_version"),
                rs.getInt("numero"),
                rs.getString("texto"),
                rs.getString("imagen_url"),
                rs.getInt("gravedad"),
                rs.getBigDecimal("peso"),
                rs.getString("explicacion"),
                rs.getBoolean("bandera_roja"),
                rs.getBigDecimal("costo_reparacion"),
                rs.getBoolean("vigente"),
                rs.getTimestamp("creado_en").toInstant());
    }
}
