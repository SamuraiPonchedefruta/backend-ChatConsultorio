package com.autobot.backend.tco.controller;

import java.time.LocalDate;
import java.util.Comparator;

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
            + "La compra usa el punto medio del rango de precio de referencia; seguro, gastos, "
            + "kilómetros por año y precio del combustible son supuestos por defecto, y el "
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
                req.anios(), req.kmPorGalon(), req.mantenimientoAnual());
        return new CalculoResponse(desglose, ALCANCE, LIMITACIONES,
                fechaDeLosDatos(req.idModelo(), req.anioModelo()));
    }

    /** La fecha más antigua entre los supuestos y el rango usado, para no aparentar datos más frescos. */
    private LocalDate fechaDeLosDatos(Long idModelo, int anioModelo) {
        LocalDate fechaSupuestos = supuestos.findAll().stream()
                .map(SupuestoTco::getFechaFuente)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate fechaRango = rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(idModelo, anioModelo)
                .get(0).getFechaFuente();
        return fechaSupuestos.isBefore(fechaRango) ? fechaSupuestos : fechaRango;
    }
}
