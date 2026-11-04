package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
import com.autobot.backend.auth.model.Bitacora;
import com.autobot.backend.auth.repository.BitacoraRepository;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
class ModeloBitacoraTest extends AbstractIntegrationTest {

    private static final String CUERPO = """
            {"marca":"Toyota","nombre":"Hilux","consumo":"9 l/100 km",
             "mantenimiento":"Aceite cada 10 000 km","fallasComunes":"Embrague",
             "repuestos":"Filtro de aire","queRevisar":"Suspensión"}""";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    BitacoraRepository bitacora;

    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        modelos.deleteAll();
        em.flush();
    }

    @Test
    void altaDeModeloQuedaRegistradaConUsuarioYFecha() throws Exception {
        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());

        Modelo creado = modelos.findByMarcaIgnoreCaseAndNombreIgnoreCase("Toyota", "Hilux").orElseThrow();

        assertThat(bitacora.findByIdModeloOrderByFechaDesc(creado.getId()))
                .singleElement()
                .satisfies(registro -> {
                    assertThat(registro.getAccion()).isEqualTo(Bitacora.Accion.MODELO_ALTA);
                    assertThat(registro.getCorreoAdmin()).isEqualTo("admin@example.com");
                    assertThat(registro.getFecha()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
                    assertThat(registro.getDetalle()).isEqualTo("Toyota Hilux");
                    assertThat(registro.getCorreoAfectado()).isNull();
                });
    }

    @Test
    void cadaEdicionDeModeloQuedaRegistrada() throws Exception {
        Modelo existente = modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));

        mockMvc.perform(put("/admin/modelos/{id}", existente.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/modelos/{id}", existente.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());

        assertThat(bitacora.findByIdModeloOrderByFechaDesc(existente.getId()))
                .hasSize(2)
                .allSatisfy(registro -> {
                    assertThat(registro.getAccion()).isEqualTo(Bitacora.Accion.MODELO_EDICION);
                    assertThat(registro.getCorreoAdmin()).isEqualTo("admin@example.com");
                    assertThat(registro.getFecha()).isNotNull();
                });
    }

    @Test
    void unaAltaRechazadaNoDejaRegistro() throws Exception {
        modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));
        long antes = bitacora.count();

        mockMvc.perform(post("/admin/modelos")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());

        assertThat(bitacora.count()).isEqualTo(antes);
    }
}
