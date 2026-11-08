package com.autobot.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "JWT_SECRET=clave-de-prueba-solo-para-tests-que-supera-los-32-bytes",
    "app.jwt.issuer=autobot-backend",
    "app.jwt.expiration-minutes=60"
})
class ApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring Boot cargue correctamente con todos los beans, seguridad y JWT
    }
}