package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Year;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RangoPrecioTest extends AbstractIntegrationTest {

    private static final String URL = "/admin/modelos/{id}/rangos-precio";

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
    }

    private static String cuerpo(int anio, String min, String max) {
        return """
                {"anio":%d,"precioMin":%s,"precioMax":%s,
                 "fuente":"Corotos Panamá","fechaFuente":"2026-10-01"}""".formatted(anio, min, max);
    }

    private String rechazo400(String json) throws Exception {
        return mockMvc.perform(post(URL, modelo.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminGuardaElRangoConFuenteYFecha() throws Exception {
        mockMvc.perform(post(URL, modelo.getId())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(2018, "15000.00", "20000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.idModelo").value(modelo.getId()))
                .andExpect(jsonPath("$.fuente").value("Corotos Panamá"))
                .andExpect(jsonPath("$.fechaFuente").value("2026-10-01"));

        em.flush();
        em.clear();
        assertThat(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(modelo.getId(), 2018))
                .singleElement()
                .satisfies(guardado -> {
                    assertThat(guardado.getPrecioMin()).isEqualByComparingTo("15000.00");
                    assertThat(guardado.getPrecioMax()).isEqualByComparingTo("20000.00");
                    assertThat(guardado.getFuente()).isEqualTo("Corotos Panamá");
                });
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void noAdministradorRecibe403() throws Exception {
        mockMvc.perform(post(URL, modelo.getId())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(2018, "15000.00", "20000.00")))
                .andExpect(status().isForbidden());

        assertThat(rangos.count()).isZero();
    }

    @Test
    void anonimoRecibe401() throws Exception {
        mockMvc.perform(post(URL, modelo.getId())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(2018, "15000.00", "20000.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void modeloInexistenteResponde404() throws Exception {
        mockMvc.perform(post(URL, 999999)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(2018, "15000.00", "20000.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void minimoMayorQueMaximoSeRechazaConMensajeClaro() throws Exception {
        String respuesta = rechazo400(cuerpo(2018, "20000.00", "15000.00"));

        assertThat(respuesta).contains("mayor que");
        assertThat(rangos.count()).isZero();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void anioAnteriorAlMinimoSeRechazaConMensajeClaro() throws Exception {
        String respuesta = rechazo400(cuerpo(1850, "15000.00", "20000.00"));

        assertThat(respuesta).contains("1900");
        assertThat(rangos.count()).isZero();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void anioMuyFuturoSeRechazaConMensajeClaro() throws Exception {
        int demasiadoFuturo = Year.now().getValue() + 2;

        String respuesta = rechazo400(cuerpo(demasiadoFuturo, "15000.00", "20000.00"));

        assertThat(respuesta).contains(String.valueOf(Year.now().getValue() + 1));
        assertThat(rangos.count()).isZero();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void fuenteVaciaSeRechaza() throws Exception {
        rechazo400("""
                {"anio":2018,"precioMin":15000.00,"precioMax":20000.00,
                 "fuente":"","fechaFuente":"2026-10-01"}""");

        assertThat(rangos.count()).isZero();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void precioNegativoSeRechaza() throws Exception {
        rechazo400(cuerpo(2018, "-1.00", "20000.00"));

        assertThat(rangos.count()).isZero();
    }
}
