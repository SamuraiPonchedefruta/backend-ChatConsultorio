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
class CalculadoraApiTest extends AbstractIntegrationTest {

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

    private String cuerpo(Long idModelo, int anios, String kmPorGalon) {
        return """
                {"idModelo": %d, "anioModelo": %d, "anios": %d,
                 "kmPorGalon": %s, "mantenimientoAnual": 300}""".formatted(idModelo, ANIO, anios, kmPorGalon);
    }

    @Test
    void respondeSinTokenConElDesgloseDeLosCincoRubros() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(modelo.getId(), 5, "30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.anios").value(5))
                .andExpect(jsonPath("$.desglose.compra").value(17500.0))
                .andExpect(jsonPath("$.desglose.combustible").value(9500.0))
                .andExpect(jsonPath("$.desglose.mantenimiento").value(1500.0))
                .andExpect(jsonPath("$.desglose.seguro").value(3000.0))
                .andExpect(jsonPath("$.desglose.gastosAnuales").value(2000.0))
                .andExpect(jsonPath("$.desglose.total").value(33500.0));
    }

    @Test
    void incluyeAlcanceLimitacionesYFechaDeLosDatos() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(modelo.getId(), 5, "30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").isNotEmpty())
                .andExpect(jsonPath("$.limitaciones").isNotEmpty())
                .andExpect(jsonPath("$.fechaDatos").value("2026-10-01"));
    }

    @Test
    void unTokenInvalidoNoImpideElCalculoPublico() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token.invalido.cualquiera")
                .content(cuerpo(modelo.getId(), 1, "30")))
                .andExpect(status().isOk());
    }

    @Test
    void responde404SiElModeloNoTieneRangoDePrecio() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(modelo.getId() + 999, 5, "30")))
                .andExpect(status().isNotFound());
    }

    @Test
    void responde400SiElHorizonteEstaFueraDeRango() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(modelo.getId(), 21, "30")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responde400SiElConsumoEsCero() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(modelo.getId(), 5, "0")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responde400SiFaltanCampos() throws Exception {
        mockMvc.perform(post("/calculadora/tco").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
