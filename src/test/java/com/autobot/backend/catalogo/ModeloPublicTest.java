package com.autobot.backend.catalogo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModeloPublicTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    EntityManager em;

    private Modelo modelo;

    @BeforeEach
    void setUp() {
        modelos.deleteAll();
        em.flush();

        Modelo nuevo = new Modelo("Toyota", "Hilux");
        nuevo.asignarFicha(new FichaModelo(
                "9 l/100 km", "Aceite cada 10000 km", "Desgaste del embrague",
                "Filtro de aire", "Revisar la suspension"));
        modelo = modelos.saveAndFlush(nuevo);
    }

    @Test
    void devuelveLaFichaCompletaSinToken() throws Exception {
        mockMvc.perform(get("/modelos/{id}", modelo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelo.getId()))
                .andExpect(jsonPath("$.marca").value("Toyota"))
                .andExpect(jsonPath("$.nombre").value("Hilux"))
                .andExpect(jsonPath("$.consumo").value("9 l/100 km"))
                .andExpect(jsonPath("$.mantenimiento").value("Aceite cada 10000 km"))
                .andExpect(jsonPath("$.fallasComunes").value("Desgaste del embrague"))
                .andExpect(jsonPath("$.repuestos").value("Filtro de aire"))
                .andExpect(jsonPath("$.queRevisar").value("Revisar la suspension"))
                .andExpect(jsonPath("$.actualizadoEn").exists());
    }

    @Test
    void unTokenInvalidoNoImpideLaConsultaPublica() throws Exception {
        mockMvc.perform(get("/modelos/{id}", modelo.getId())
                .header("Authorization", "Bearer token.invalido.cualquiera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Toyota"));
    }

    @Test
    void unModeloSinFichaDevuelveLosCamposVacios() throws Exception {
        Modelo sinFicha = modelos.saveAndFlush(new Modelo("Nissan", "Frontier"));

        mockMvc.perform(get("/modelos/{id}", sinFicha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Nissan"))
                .andExpect(jsonPath("$.consumo").doesNotExist())
                .andExpect(jsonPath("$.actualizadoEn").exists());
    }

    @Test
    void unModeloInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/modelos/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElGetEsPublico() throws Exception {
        mockMvc.perform(post("/modelos/{id}", modelo.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elCambioSeVeSinRedespliegue() throws Exception {
        modelo.getFicha().actualizar("7 l/100 km", "Aceite cada 5000 km", "Ninguna",
                "Pastillas de freno", "Revisar los frenos");
        modelos.saveAndFlush(modelo);

        mockMvc.perform(get("/modelos/{id}", modelo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumo").value("7 l/100 km"))
                .andExpect(jsonPath("$.queRevisar").value("Revisar los frenos"));
    }
}
