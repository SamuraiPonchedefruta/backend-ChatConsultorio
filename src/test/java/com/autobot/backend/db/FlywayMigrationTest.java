package com.autobot.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Reconstruye el esquema desde cero: levanta un PostgreSQL vacío y efímero,
 * deja que Flyway aplique todas las migraciones al arrancar el contexto
 * y verifica el historial registrado.
 */
@SpringBootTest
@Testcontainers
class FlywayMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway aplica las migraciones sobre una BD vacía y registra el historial")
    void aplicaMigracionesDesdeCero() {
        List<Map<String, Object>> historial = jdbc.queryForList(
                "SELECT installed_rank, version, description, success "
                        + "FROM flyway_schema_history "
                        + "WHERE version IS NOT NULL "
                        + "ORDER BY installed_rank");

        // Se aplicó al menos la migración inicial
        assertThat(historial).isNotEmpty();

        // Todas terminaron con éxito
        assertThat(historial).allSatisfy(fila ->
                assertThat(fila.get("success")).isEqualTo(true));

        // La primera es V1__base
        assertThat(historial.get(0).get("version")).isEqualTo("1");
        assertThat(historial.get(0).get("description")).isEqualTo("base");

        // El orden de ejecución coincide con el orden de versión
        List<Integer> versiones = historial.stream()
                .map(fila -> Integer.parseInt((String) fila.get("version")))
                .toList();
        assertThat(versiones).isSorted();
    }
}
