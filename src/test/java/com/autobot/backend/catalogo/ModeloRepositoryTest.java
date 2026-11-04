package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ModeloRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    ModeloRepository modelos;

    @Autowired
    EntityManager em;

    @Test
    void guardaModeloConActualizadoEnAutomatico() {
        Modelo guardado = modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getActualizadoEn())
                .isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void guardaLaFichaJuntoConElModelo() {
        Modelo modelo = new Modelo("Toyota", "Hilux");
        modelo.asignarFicha(new FichaModelo(
                "9 l/100 km", "Cambio de aceite cada 10 000 km",
                "Desgaste del embrague", "Filtro de aire", "Revisar la suspensión"));
        modelos.saveAndFlush(modelo);
        em.clear();

        Modelo leido = modelos.findById(modelo.getId()).orElseThrow();

        assertThat(leido.getFicha()).isNotNull();
        assertThat(leido.getFicha().getConsumo()).isEqualTo("9 l/100 km");
        assertThat(leido.getFicha().getQueRevisar()).isEqualTo("Revisar la suspensión");
        assertThat(leido.getFicha().getActualizadoEn()).isNotNull();
    }

    @Test
    void rechazaUnModeloDuplicado() {
        modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));

        assertThatThrownBy(() -> modelos.saveAndFlush(new Modelo("Toyota", "Hilux")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
