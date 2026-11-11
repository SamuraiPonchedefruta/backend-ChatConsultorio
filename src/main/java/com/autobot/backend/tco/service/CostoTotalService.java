package com.autobot.backend.tco.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;
import com.autobot.backend.tco.dto.CostoTotalResultado;
import com.autobot.backend.tco.repository.SupuestoTcoRepository;

/** Calcula el costo total de propiedad (TCO) con cinco rubros: compra, combustible, mantenimiento, seguro y gastos. */
@Service
public class CostoTotalService {

    static final int ANIOS_MINIMO = 1;
    static final int ANIOS_MAXIMO = 20;
    private static final int ESCALA = 2;

    private final SupuestoTcoRepository supuestos;
    private final RangoPrecioRepository rangos;

    public CostoTotalService(SupuestoTcoRepository supuestos, RangoPrecioRepository rangos) {
        this.supuestos = supuestos;
        this.rangos = rangos;
    }

    /** Calcula solo con los supuestos por defecto de supuesto_tco. */
    @Transactional(readOnly = true)
    public CostoTotalResultado calcular(Long idModelo, int anioModelo, int anios,
            BigDecimal kmPorGalon, BigDecimal mantenimientoAnual) {
        return calcular(idModelo, anioModelo, anios, kmPorGalon, mantenimientoAnual, null, null, null);
    }

    /**
     * @param kmPorGalon         consumo del vehículo; la ficha solo lo trae como texto, por eso entra aquí
     * @param mantenimientoAnual costo de mantenimiento por año, por la misma razón
     * @param kmPorAnio          supuesto del usuario; si es null se usa KM_POR_ANIO
     * @param precioCombustible  supuesto del usuario; si es null se usa PRECIO_COMBUSTIBLE
     * @param seguroAnual        supuesto del usuario; si es null se usa SEGURO_ANUAL
     */
    @Transactional(readOnly = true)
    public CostoTotalResultado calcular(Long idModelo, int anioModelo, int anios,
            BigDecimal kmPorGalon, BigDecimal mantenimientoAnual,
            BigDecimal kmPorAnio, BigDecimal precioCombustible, BigDecimal seguroAnual) {
        validar(anios, kmPorGalon, mantenimientoAnual);

        BigDecimal horizonte = BigDecimal.valueOf(anios);
        BigDecimal compra = precioDeCompra(idModelo, anioModelo);

        BigDecimal km = kmPorAnio != null ? kmPorAnio : valor("KM_POR_ANIO");
        BigDecimal precio = precioCombustible != null ? precioCombustible : valor("PRECIO_COMBUSTIBLE");
        BigDecimal seguroPorAnio = seguroAnual != null ? seguroAnual : valor("SEGURO_ANUAL");

        // Se multiplica todo antes de dividir, para redondear una sola vez al final.
        BigDecimal combustible = km.multiply(precio).multiply(horizonte)
                .divide(kmPorGalon, ESCALA, RoundingMode.HALF_UP);

        BigDecimal mantenimiento = mantenimientoAnual.multiply(horizonte).setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal seguro = seguroPorAnio.multiply(horizonte).setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal gastos = valor("GASTOS_ANUALES").multiply(horizonte).setScale(ESCALA, RoundingMode.HALF_UP);

        return CostoTotalResultado.de(anios, compra, combustible, mantenimiento, seguro, gastos);
    }

    /** Punto medio del rango de precio más reciente del modelo para ese año. */
    private BigDecimal precioDeCompra(Long idModelo, int anioModelo) {
        List<RangoPrecio> disponibles = rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(idModelo, anioModelo);
        if (disponibles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay rango de precio para ese modelo y año");
        }
        RangoPrecio masReciente = disponibles.get(0);
        return masReciente.getPrecioMin().add(masReciente.getPrecioMax())
                .divide(BigDecimal.valueOf(2), ESCALA, RoundingMode.HALF_UP);
    }

    private BigDecimal valor(String clave) {
        return supuestos.findByClave(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Falta el supuesto del TCO: " + clave))
                .getValor();
    }

    private void validar(int anios, BigDecimal kmPorGalon, BigDecimal mantenimientoAnual) {
        if (anios < ANIOS_MINIMO || anios > ANIOS_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El horizonte debe estar entre " + ANIOS_MINIMO + " y " + ANIOS_MAXIMO + " años");
        }
        if (kmPorGalon == null || kmPorGalon.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El consumo (km por galón) debe ser mayor que cero");
        }
        if (mantenimientoAnual == null || mantenimientoAnual.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El mantenimiento anual no puede ser negativo");
        }
    }
}
