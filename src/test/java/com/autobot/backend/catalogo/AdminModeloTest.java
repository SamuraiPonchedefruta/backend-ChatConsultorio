package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminModeloTest extends AbstractIntegrationTest {

    private static final String CUERPO = """
            {"marca":"Toyota","nombre":"Hilux","consumo":"9 l/100 km",
             "mantenimiento":"Aceite cada 10 000 km","fallasComunes":"Embrague",
             "repuestos":"Filtro de aire","queRevisar":"Suspensión"}""";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        modelos.deleteAll();
        em.flush();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCreaModeloYResponde200() throws Exception {
        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.marca").value("Toyota"))
                .andExpect(jsonPath("$.queRevisar").value("Suspensión"))
                .andExpect(jsonPath("$.actualizadoEn").exists());

        assertThat(modelos.findByMarcaIgnoreCaseAndNombreIgnoreCase("toyota", "hilux")).isPresent();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminEditaModeloYElCambioQuedaGuardado() throws Exception {
        Modelo existente = modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));

        mockMvc.perform(put("/admin/modelos/{id}", existente.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumo").value("9 l/100 km"));

        em.flush();
        em.clear();
        Modelo leido = modelos.findById(existente.getId()).orElseThrow();
        assertThat(leido.getFicha()).isNotNull();
        assertThat(leido.getFicha().getFallasComunes()).isEqualTo("Embrague");
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void noAdministradorRecibe403() throws Exception {
        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());

        assertThat(modelos.count()).isZero();
    }

    @Test
    void anonimoRecibe401() throws Exception {
        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void crearUnModeloDuplicadoResponde409() throws Exception {
        modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));

        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void editarUnModeloInexistenteResponde404() throws Exception {
        mockMvc.perform(put("/admin/modelos/{id}", 999999)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void marcaVaciaResponde400() throws Exception {
        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"marca\":\"\",\"nombre\":\"Hilux\"}"))
                .andExpect(status().isBadRequest());
    }
}
