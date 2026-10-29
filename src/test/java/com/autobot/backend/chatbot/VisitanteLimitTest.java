package com.autobot.backend.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisitanteLimitTest {

    private VisitanteLimitService visitanteLimitService;

    @BeforeEach
    void setUp() {
        // Configuramos un límite pequeño de 3 para pruebas rápidas
        visitanteLimitService = new VisitanteLimitService(3);
    }

    @Test
    void testPermitirInteraccionBajoLimite() {
        String ipAnonima = "192.168.1.50";

        assertTrue(visitanteLimitService.permitirInteraccion(ipAnonima));
        visitanteLimitService.registrarInteraccion(ipAnonima);

        assertTrue(visitanteLimitService.permitirInteraccion(ipAnonima));
        visitanteLimitService.registrarInteraccion(ipAnonima);

        assertTrue(visitanteLimitService.permitirInteraccion(ipAnonima));
        visitanteLimitService.registrarInteraccion(ipAnonima);
    }

    @Test
    void testExcesoDeLimiteDiario() {
        String ipAnonima = "10.0.0.15";

        // Consumimos las 3 interacciones permitidas
        for (int i = 0; i < 3; i++) {
            assertTrue(visitanteLimitService.permitirInteraccion(ipAnonima));
            visitanteLimitService.registrarInteraccion(ipAnonima);
        }

        // La cuarta interacción debe ser denegada
        assertFalse(visitanteLimitService.permitirInteraccion(ipAnonima));
    }
}
