package com.autobot.backend.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

@Service
public class ComparadorService {

    static final int MIN_MODELOS = 2;

    private final ModeloRepository modeloRepository;
    private final RangoPrecioRepository rangoPrecioRepository;
    private final int maxModelos;

    public ComparadorService(ModeloRepository modeloRepository,
            RangoPrecioRepository rangoPrecioRepository,
            @Value("${comparador.max-modelos:4}") int maxModelos) {
        if (maxModelos < MIN_MODELOS) {
            throw new IllegalStateException(
                    "comparador.max-modelos debe ser >= " + MIN_MODELOS + " (valor: " + maxModelos + ")");
        }
        this.modeloRepository = modeloRepository;
        this.rangoPrecioRepository = rangoPrecioRepository;
        this.maxModelos = maxModelos;
    }

    @Transactional(readOnly = true)
    public Comparacion comparar(List<Long> ids, Integer anio) {
        validarRango(ids);

        Map<Long, Modelo> porId = modeloRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Modelo::getId, Function.identity()));

        List<Long> faltantes = ids.stream().filter(id -> !porId.containsKey(id)).toList();
        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Modelos no encontrados: " + faltantes);
        }

        // Mismo orden que pidió el usuario; todos los modelos traen los mismos campos
        List<ModeloComparado> modelos = ids.stream()
                .map(porId::get)
                .map(m -> aComparado(m, anio))
                .toList();

        return new Comparacion(ids, anio, modelos);
    }

    private ModeloComparado aComparado(Modelo m, Integer anio) {
        return new ModeloComparado(
                m.getId(),
                m.getMarca(),
                m.getNombre(),
                m.getActualizadoEn(),
                aFicha(m.getFicha()),
                precioDe(m.getId(), anio));
    }

    private FichaComparada aFicha(FichaModelo f) {
        if (f == null) {
            return null;
        }
        return new FichaComparada(f.getConsumo(), f.getMantenimiento(),
                f.getFallasComunes(), f.getRepuestos(), f.getQueRevisar());
    }

    private PrecioComparado precioDe(Long idModelo, Integer anio) {
        if (anio == null) {
            return null;
        }
        // La consulta ya viene ordenada: el primero es el de fuente más reciente
        return rangoPrecioRepository
                .findByModeloIdAndAnioOrderByFechaFuenteDesc(idModelo, anio).stream()
                .findFirst()
                .map(r -> new PrecioComparado(r.getAnio(), r.getPrecioMin(), r.getPrecioMax(),
                        r.getFuente(), r.getFechaFuente()))
                .orElse(null);
    }

    private void validarRango(List<Long> ids) {
        if (ids == null || ids.size() < MIN_MODELOS || ids.size() > maxModelos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes comparar entre " + MIN_MODELOS + " y " + maxModelos + " modelos");
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La lista de ids contiene valores vacíos");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No repitas ids en la comparación");
        }
    }

    public record FichaComparada(String consumo, String mantenimiento, String fallasComunes,
            String repuestos, String queRevisar) {}

    public record PrecioComparado(Integer anio, BigDecimal precioMin, BigDecimal precioMax,
            String fuente, LocalDate fechaFuente) {}

    public record ModeloComparado(Long id, String marca, String nombre, Instant actualizadoEn,
            FichaComparada ficha, PrecioComparado precio) {}

    public record Comparacion(List<Long> ids, Integer anio, List<ModeloComparado> modelos) {}
}
