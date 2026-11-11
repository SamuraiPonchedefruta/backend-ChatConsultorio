package com.autobot.backend.tco;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CalculoValidacionTest extends AbstractIntegrationTest {

    private static final int ANIO = 2018;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    RangoPrecioRepository rangos;

    @Autowired
    EntityManager em;

    private Modelo modelo;

    @BeforeEach
    void setUp() {
        modelos.deleteAll();
        em.flush();
        modelo = modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));
        rangos.saveAndFlush(new RangoPrecio(modelo, ANIO, new BigDecimal("15000.00"),
                new BigDecimal("20000.00"), "Prueba", LocalDate.of(2026, 10, 1)));
    }

    /** Cuerpo base a 5 años; "extra" agrega los supuestos del usuario, por ejemplo: , "kmPorAnio": 0 */
    private String cuerpo(String extra) {
        return """
                {"idModelo": %d, "anioModelo": %d, "anios": 5,
                 "kmPorGalon": 30, "mantenimientoAnual": 300%s}""".formatted(modelo.getId(), ANIO, extra);
    }

    private void esperar400(String extra, String detalle) throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(extra)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(detalle));
    }

    @Test
    void unValorValidoRecalculaElResultado() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(", \"kmPorAnio\": 10000, \"precioCombustible\": 4.00, \"seguroAnual\": 500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.compra").value(17500.0))
                .andExpect(jsonPath("$.desglose.combustible").value(6666.67))
                .andExpect(jsonPath("$.desglose.mantenimiento").value(1500.0))
                .andExpect(jsonPath("$.desglose.seguro").value(2500.0))
                .andExpect(jsonPath("$.desglose.gastosAnuales").value(2000.0))
                .andExpect(jsonPath("$.desglose.total").value(30166.67));
    }

    @Test
    void sinSupuestosDelUsuarioUsaLosPorDefecto() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.total").value(33500.0));
    }

    @Test
    void aceptaUnSeguroEnCero() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(", \"seguroAnual\": 0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.seguro").value(0.0))
                .andExpect(jsonPath("$.desglose.total").value(30500.0));
    }

    @Test
    void rechazaKilometrosPorAnioEnCero() throws Exception {
        esperar400(", \"kmPorAnio\": 0",
                "kmPorAnio: Los kilómetros por año deben ser mayores que cero");
    }

    @Test
    void rechazaKilometrosPorAnioMuyAltos() throws Exception {
        esperar400(", \"kmPorAnio\": 200001",
                "kmPorAnio: Los kilómetros por año no pueden superar 200000");
    }

    @Test
    void rechazaUnPrecioDeCombustibleNegativo() throws Exception {
        esperar400(", \"precioCombustible\": -1",
                "precioCombustible: El precio del combustible debe ser mayor que cero");
    }

    @Test
    void rechazaUnPrecioDeCombustibleMuyAlto() throws Exception {
        esperar400(", \"precioCombustible\": 101",
                "precioCombustible: El precio del combustible no puede superar 100 USD por galón");
    }

    @Test
    void rechazaUnSeguroNegativo() throws Exception {
        esperar400(", \"seguroAnual\": -5",
                "seguroAnual: El seguro anual no puede ser negativo");
    }

    @Test
    void rechazaUnSeguroMuyAlto() throws Exception {
        esperar400(", \"seguroAnual\": 100001",
                "seguroAnual: El seguro anual no puede superar 100000 USD");
    }

    @Test
    void rechazaUnValorQueNoEsNumero() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(", \"kmPorAnio\": \"mucho\"")))
                .andExpect(status().isBadRequest());
    }
}
