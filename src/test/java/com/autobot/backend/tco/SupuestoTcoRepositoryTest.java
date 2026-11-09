package com.autobot.backend.tco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.tco.model.SupuestoTco;
import com.autobot.backend.tco.repository.SupuestoTcoRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class SupuestoTcoRepositoryTest extends AbstractIntegrationTest {

    private static final String INSERT = """
            INSERT INTO supuesto_tco (clave, descripcion, valor, unidad, fuente, fecha_fuente)
            VALUES (?, ?, ?, ?, ?, ?)""";

    @Autowired
    SupuestoTcoRepository supuestos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager em;

    @Test
    void flywayAplicoLaV20SinErrores() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20' AND success = TRUE",
                Integer.class);

        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void laMigracionSiembraLosCuatroSupuestosPorDefecto() {
        assertThat(supuestos.findAllByOrderByClaveAsc())
                .extracting(SupuestoTco::getClave)
                .containsExactly("GASTOS_ANUALES", "KM_POR_ANIO", "PRECIO_COMBUSTIBLE", "SEGURO_ANUAL");
    }

    @Test
    void cadaSupuestoGuardaLaFechaDeSusDatos() {
        assertThat(supuestos.findAll())
                .isNotEmpty()
                .allSatisfy(s -> {
                    assertThat(s.getFechaFuente()).isNotNull();
                    assertThat(s.getFuente()).isNotBlank();
                });
    }

    @Test
    void seEditaConUnUpdateSinTocarCodigo() {
        jdbc.update("UPDATE supuesto_tco SET valor = ?, fuente = ?, fecha_fuente = ? WHERE clave = ?",
                new BigDecimal("4.25"), "Estación de servicio local", LocalDate.of(2026, 11, 1),
                "PRECIO_COMBUSTIBLE");
        em.clear();

        SupuestoTco leido = supuestos.findByClave("PRECIO_COMBUSTIBLE").orElseThrow();

        assertThat(leido.getValor()).isEqualByComparingTo("4.25");
        assertThat(leido.getFuente()).isEqualTo("Estación de servicio local");
        assertThat(leido.getFechaFuente()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void actualizarCambiaElValorYLaFechaJuntos() {
        SupuestoTco km = supuestos.findByClave("KM_POR_ANIO").orElseThrow();

        km.actualizar(new BigDecimal("12000.00"), "  Encuesta propia  ", LocalDate.of(2026, 11, 5));
        supuestos.saveAndFlush(km);
        em.clear();

        SupuestoTco leido = supuestos.findByClave("KM_POR_ANIO").orElseThrow();
        assertThat(leido.getValor()).isEqualByComparingTo("12000.00");
        assertThat(leido.getFuente()).isEqualTo("Encuesta propia");
        assertThat(leido.getFechaFuente()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void laEntidadRechazaUnValorNegativo() {
        SupuestoTco km = supuestos.findByClave("KM_POR_ANIO").orElseThrow();

        assertThatThrownBy(() -> km.actualizar(new BigDecimal("-1"), "Fuente", LocalDate.of(2026, 11, 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laBaseDeDatosRechazaUnValorNegativo() {
        assertThatThrownBy(() -> jdbc.update(INSERT, "NEGATIVO", "Prueba", new BigDecimal("-5.00"),
                "USD", "Prueba", LocalDate.of(2026, 11, 5)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_supuesto_tco_valor");
    }

    @Test
    void laBaseDeDatosRechazaUnaClaveRepetida() {
        assertThatThrownBy(() -> jdbc.update(INSERT, "KM_POR_ANIO", "Repetida", new BigDecimal("1.00"),
                "km/año", "Prueba", LocalDate.of(2026, 11, 5)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_supuesto_tco_clave");
    }
}
