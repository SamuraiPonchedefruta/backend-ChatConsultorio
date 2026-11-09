package com.autobot.backend.tco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;
import com.autobot.backend.tco.dto.CostoTotalResultado;
import com.autobot.backend.tco.model.SupuestoTco;
import com.autobot.backend.tco.repository.SupuestoTcoRepository;
import com.autobot.backend.tco.service.CostoTotalService;

@ExtendWith(MockitoExtension.class)
class CostoTotalServiceTest {

    private static final Long ID_MODELO = 1L;
    private static final int ANIO = 2018;
    private static final BigDecimal KM_POR_GALON = new BigDecimal("30");
    private static final BigDecimal MANTENIMIENTO = new BigDecimal("300");

    @Mock
    SupuestoTcoRepository supuestos;

    @Mock
    RangoPrecioRepository rangos;

    private CostoTotalService servicio() {
        return new CostoTotalService(supuestos, rangos);
    }

    private void supuesto(String clave, String valor) {
        when(supuestos.findByClave(clave)).thenReturn(Optional.of(new SupuestoTco(clave, "Prueba",
                new BigDecimal(valor), "u", "Prueba", LocalDate.of(2026, 11, 9))));
    }

    private RangoPrecio rango(String min, String max, LocalDate fecha) {
        return new RangoPrecio(new Modelo("Toyota", "Hilux"), ANIO,
                new BigDecimal(min), new BigDecimal(max), "Prueba", fecha);
    }

    /** Deja listos los cuatro supuestos sembrados en V20 y un rango de 15000 a 20000. */
    private void datosNormales() {
        supuesto("KM_POR_ANIO", "15000.00");
        supuesto("PRECIO_COMBUSTIBLE", "3.80");
        supuesto("SEGURO_ANUAL", "600.00");
        supuesto("GASTOS_ANUALES", "400.00");
        when(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(ID_MODELO, ANIO))
                .thenReturn(List.of(rango("15000.00", "20000.00", LocalDate.of(2026, 10, 1))));
    }

    @Test
    void devuelveElDesgloseDeLosCincoRubrosACincoAnios() {
        datosNormales();

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 5, KM_POR_GALON, MANTENIMIENTO);

        assertThat(r.anios()).isEqualTo(5);
        assertThat(r.compra()).isEqualByComparingTo("17500.00");
        assertThat(r.combustible()).isEqualByComparingTo("9500.00");
        assertThat(r.mantenimiento()).isEqualByComparingTo("1500.00");
        assertThat(r.seguro()).isEqualByComparingTo("3000.00");
        assertThat(r.gastosAnuales()).isEqualByComparingTo("2000.00");
        assertThat(r.total()).isEqualByComparingTo("33500.00");
    }

    @Test
    void elTotalEsLaSumaDeLosCincoRubros() {
        datosNormales();

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 3, KM_POR_GALON, MANTENIMIENTO);

        assertThat(r.total()).isEqualByComparingTo(
                r.compra().add(r.combustible()).add(r.mantenimiento()).add(r.seguro()).add(r.gastosAnuales()));
    }

    @Test
    void usaElRangoMasRecienteDelModelo() {
        datosNormales();
        when(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(ID_MODELO, ANIO)).thenReturn(List.of(
                rango("10000.00", "12000.00", LocalDate.of(2026, 10, 1)),
                rango("30000.00", "40000.00", LocalDate.of(2025, 1, 1))));

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 1, KM_POR_GALON, MANTENIMIENTO);

        assertThat(r.compra()).isEqualByComparingTo("11000.00");
    }

    @Test
    void limiteDeUnAnio() {
        datosNormales();

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 1, KM_POR_GALON, MANTENIMIENTO);

        assertThat(r.combustible()).isEqualByComparingTo("1900.00");
        assertThat(r.total()).isEqualByComparingTo("20700.00");
    }

    @Test
    void limiteDeVeinteAnios() {
        datosNormales();

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 20, KM_POR_GALON, MANTENIMIENTO);

        assertThat(r.combustible()).isEqualByComparingTo("38000.00");
        assertThat(r.total()).isEqualByComparingTo("81500.00");
    }

    @Test
    void redondeaElCombustibleADosDecimales() {
        datosNormales();

        // 15000 * 3.80 / 7 = 8142.857...
        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 1, new BigDecimal("7"), MANTENIMIENTO);

        assertThat(r.combustible()).isEqualTo(new BigDecimal("8142.86"));
    }

    @Test
    void aceptaMantenimientoCero() {
        datosNormales();

        CostoTotalResultado r = servicio().calcular(ID_MODELO, ANIO, 5, KM_POR_GALON, BigDecimal.ZERO);

        assertThat(r.mantenimiento()).isEqualByComparingTo("0.00");
    }

    @Test
    void rechazaUnHorizonteFueraDeRango() {
        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 0, KM_POR_GALON, MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 21, KM_POR_GALON, MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rechazaUnConsumoCeroONegativo() {
        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 5, BigDecimal.ZERO, MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 5, new BigDecimal("-1"), MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rechazaUnMantenimientoNegativo() {
        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 5, KM_POR_GALON, new BigDecimal("-1")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void fallaSiElModeloNoTieneRangoDePrecio() {
        when(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(ID_MODELO, ANIO)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 5, KM_POR_GALON, MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void fallaSiFaltaUnSupuesto() {
        supuesto("KM_POR_ANIO", "15000.00");
        supuesto("PRECIO_COMBUSTIBLE", "3.80");
        when(supuestos.findByClave("SEGURO_ANUAL")).thenReturn(Optional.empty());
        when(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(ID_MODELO, ANIO))
                .thenReturn(List.of(rango("15000.00", "20000.00", LocalDate.of(2026, 10, 1))));

        assertThatThrownBy(() -> servicio().calcular(ID_MODELO, ANIO, 5, KM_POR_GALON, MANTENIMIENTO))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(e.getReason()).contains("SEGURO_ANUAL");
                });
    }
}
