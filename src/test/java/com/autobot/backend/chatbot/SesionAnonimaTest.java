package com.autobot.backend.chatbot;

import com.autobot.backend.auth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SesionAnonimaTest {

    private SesionAnonimaService sesionAnonimaService;

    @BeforeEach
    void setUp() {
        sesionAnonimaService = new SesionAnonimaService();
    }

    @Test
    void testGuardarYRecuperarTrabajoTemporal() {
        String identificador = "anon-fingerprint-123";
        String datosTrabajo = "{\"conversacion\": \"Hola chatbot, consulta sobre cultivos\"}";

        sesionAnonimaService.guardarTrabajoTemporal(identificador, datosTrabajo);

        Optional<String> recuperado = sesionAnonimaService.obtenerTrabajoTemporal(identificador);
        assertTrue(recuperado.isPresent());
        assertEquals(datosTrabajo, recuperado.get());
    }

    @Test
    void testMigracionDeSesionAlRegistrarse() {
        String identificador = "anon-fingerprint-456";
        String datosTrabajo = "{\"progreso\": \"analisis de suelo veraguas\"}";

        sesionAnonimaService.guardarTrabajoTemporal(identificador, datosTrabajo);
        assertTrue(sesionAnonimaService.obtenerTrabajoTemporal(identificador).isPresent());

        // Simulamos el registro de un nuevo usuario
        Usuario nuevoUsuario = new Usuario("Saúl Abrego", "saul@example.com", "hashpassword");

        // Ejecutamos la migración
        sesionAnonimaService.migrarSesionAUsuario(identificador, nuevoUsuario);

        // Verificamos que el trabajo temporal anónimo fue procesado y removido de la caché temporal
        assertFalse(sesionAnonimaService.obtenerTrabajoTemporal(identificador).isPresent());
    }
}
