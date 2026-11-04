package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class RangoPrecioRepositoryTest extends AbstractIntegrationTest {

    private static final String INSERT = """
            INSERT INTO rango_precio (id_modelo, anio, precio_min, precio_max, fuente, fecha_fuente)
            VALUES (?, ?, ?, ?, ?, ?)""";

    @Autowired
    ModeloRepository modelos;

    @Autowired
    RangoPrecioRepository rangos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager em;

    private Modelo modelo;

    @BeforeEach
    void setUp() {
        modelos.deleteAll();
        modelo = modelos.saveAndFlush(new Modelo("Toyota", "Hilux"));
    }

    @Test
    void guardaElRangoConSuFuenteYSuFecha() {
        RangoPrecio guardado = rangos.saveAndFlush(new RangoPrecio(modelo, 2018,
                new BigDecimal("15000.00"), new BigDecimal("20000.00"),
                "  Corotos Panamá  ", LocalDate.of(2026, 10, 1)));
        em.clear();

        assertThat(rangos.findByModeloIdAndAnioOrderByFechaFuenteDesc(modelo.getId(), 2018))
                .singleElement()
                .satisfies(leido -> {
                    assertThat(leido.getId()).isEqualTo(guardado.getId());
                    assertThat(leido.getPrecioMin()).isEqualByComparingTo("15000.00");
                    assertThat(leido.getPrecioMax()).isEqualByComparingTo("20000.00");
                    assertThat(leido.getFuente()).isEqualTo("Corotos Panamá");
                    assertThat(leido.getFechaFuente()).isEqualTo(LocalDate.of(2026, 10, 1));
                });
    }

    @Test
    void laBaseDeDatosRechazaUnMinimoMayorQueElMaximo() {
        assertThatThrownBy(() -> jdbc.update(INSERT, modelo.getId(), 2018,
                new BigDecimal("20000.00"), new BigDecimal("15000.00"),
                "Corotos Panamá", LocalDate.of(2026, 10, 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rango_precio_min_max");
    }

    @Test
    void laBaseDeDatosAceptaUnMinimoIgualAlMaximo() {
        int filas = jdbc.update(INSERT, modelo.getId(), 2018,
                new BigDecimal("18000.00"), new BigDecimal("18000.00"),
                "Corotos Panamá", LocalDate.of(2026, 10, 1));

        assertThat(filas).isEqualTo(1);
    }

    @Test
    void borrarElModeloBorraSusRangosEnLaBaseDeDatos() {
        rangos.saveAndFlush(new RangoPrecio(modelo, 2018,
                new BigDecimal("15000.00"), new BigDecimal("20000.00"),
                "Corotos Panamá", LocalDate.of(2026, 10, 1)));
        // Se olvidan las entidades en memoria: se prueba la cascada de la base, no la de JPA.
        em.clear();

        jdbc.update("DELETE FROM modelo WHERE id_modelo = ?", modelo.getId());

        assertThat(rangos.count()).isZero();
    }
}
