package com.autobot.backend.tco.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobot.backend.catalogo.repository.RangoPrecioRepository;
import com.autobot.backend.tco.dto.CalculoRequest;
import com.autobot.backend.tco.dto.CalculoResponse;
import com.autobot.backend.tco.dto.CostoTotalResultado;
import com.autobot.backend.tco.model.SupuestoTco;
import com.autobot.backend.tco.repository.SupuestoTcoRepository;
import com.autobot.backend.tco.service.CostoTotalService;

import jakarta.validation.Valid;

/** Calculadora de TCO de acceso público: se abre en SecurityConfig con /calculadora/**. */
@RestController
@RequestMapping("/calculadora")
public class CalculadoraController {

    static final String ALCANCE = "Estimación del costo total de propiedad en USD con cinco rubros: "
            + "compra, combustible, mantenimiento, seguro y gastos anuales, para el horizonte indicado.";

    static final String LIMITACIONES = "Es una estimación orientativa, no una cotización. "
            + "La compra usa el punto medio del rango de precio de referencia. Kilómetros por año, "
            + "precio del combustible y seguro usan supuestos por defecto, salvo que quien consulta "
            + "indique los suyos; los gastos anuales siempre son un supuesto por defecto, y el "
            + "consumo y el mantenimiento los indica quien consulta. No incluye depreciación, "
            + "financiamiento ni imprevistos.";

    private final CostoTotalService servicio;
    private final SupuestoTcoRepository supuestos;
    private final RangoPrecioRepository rangos;

    public CalculadoraController(CostoTotalService servicio, SupuestoTcoRepository supuestos,
            RangoPrecioRepository rangos) {
        this.servicio = servicio;
        this.supuestos = supuestos;
        this.rangos = rangos;
    }

    @PostMapping("/tco")
    public CalculoResponse calcular(@Valid @RequestBody CalculoRequest req) {
        // Primero el cálculo: si no hay rango de precio responde 404 antes de buscar fechas.
        CostoTotalResultado desglose = servicio.calcular(req.idModelo(), req.anioModelo(),
                req.anios(), req.kmPorGalon(), req.mantenimientoAnual(),
                req.kmPorAnio(), req.precioCombustible(), req.seguroAnual());
        return new CalculoResponse(desglose, ALCANCE, LIMITACIONES, fechaDeLosDatos(req));
    }

    /**
     * La fecha más antigua entre el rango usado y los supuestos que salieron de la tabla.
     * Un valor escrito por el usuario no tiene fecha de la tabla, así que no cuenta.
     */
    private LocalDate fechaDeLosDatos(CalculoRequest req) {
        LocalDate fechaRango = rangos
                .findByModeloIdAndAnioOrderByFechaFuenteDesc(req.idModelo(), req.anioModelo())
                .get(0).getFechaFuente();
        return clavesDeLaTabla(req).stream()
                .map(supuestos::findByClave)
                .flatMap(Optional::stream)
                .map(SupuestoTco::getFechaFuente)
                .reduce(fechaRango, (a, b) -> a.isBefore(b) ? a : b);
    }

    private List<String> clavesDeLaTabla(CalculoRequest req) {
        List<String> claves = new ArrayList<>();
        claves.add("GASTOS_ANUALES");
        if (req.kmPorAnio() == null) {
            claves.add("KM_POR_ANIO");
        }
        if (req.precioCombustible() == null) {
            claves.add("PRECIO_COMBUSTIBLE");
        }
        if (req.seguroAnual() == null) {
            claves.add("SEGURO_ANUAL");
        }
        return claves;
    }
}
