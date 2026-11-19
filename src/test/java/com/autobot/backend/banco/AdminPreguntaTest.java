package com.autobot.backend.banco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.service.BitacoraService;
import com.autobot.backend.banco.dto.PreguntaRequest;
import com.autobot.backend.banco.model.Pregunta;
import com.autobot.backend.banco.model.PreguntaVersion;
import com.autobot.backend.banco.repository.PreguntaRepository;
import com.autobot.backend.banco.service.PreguntaService;

class AdminPreguntaTest {

    private final PreguntaRepository repo = mock(PreguntaRepository.class);
    private final BitacoraService bitacora = mock(BitacoraService.class);
    private final PreguntaService servicio = new PreguntaService(repo, bitacora);

    private final PreguntaRequest req = new PreguntaRequest(
            "texto nuevo", null, 3, new BigDecimal("1.5"), "expl", false, null);

    private Pregunta pregunta() {
        PreguntaVersion v1 = new PreguntaVersion(10L, 1, "texto original", null, 2,
                BigDecimal.ONE, null, false, null, true, Instant.now());
        return new Pregunta(7L, 1, "MOT-01", true, Instant.now(), List.of(v1));
    }

    @Test
    void editarCreaVersionNuevaVigente() {
        when(repo.buscarPorId(7L)).thenReturn(Optional.of(pregunta()));
        when(repo.agregarVersion(anyLong(), any(PreguntaVersion.class))).thenAnswer(i -> {
            PreguntaVersion n = i.getArgument(1);
            return new PreguntaVersion(11L, 2, n.texto(), n.imagenUrl(), n.gravedad(), n.peso(),
                    n.explicacion(), n.banderaRoja(), n.costoReparacion(), true, Instant.now());
        });

        PreguntaVersion nueva = servicio.editar(7L, req, "admin@correo.com");

        assertEquals(2, nueva.numero());
        assertEquals("texto nuevo", nueva.texto());
        verify(repo).agregarVersion(eq(7L), any(PreguntaVersion.class));
    }

    @Test
    void editarRegistraEnBitacoraConElCorreoDelAdmin() {
        when(repo.buscarPorId(7L)).thenReturn(Optional.of(pregunta()));
        when(repo.agregarVersion(anyLong(), any(PreguntaVersion.class))).thenAnswer(i -> {
            PreguntaVersion n = i.getArgument(1);
            return new PreguntaVersion(11L, 2, n.texto(), null, n.gravedad(), n.peso(),
                    null, false, null, true, Instant.now());
        });

        servicio.editar(7L, req, "admin@correo.com");

        verify(bitacora).registrarEdicionPregunta(eq("admin@correo.com"), eq(7L), contains("v2"));
    }

    @Test
    void editarPreguntaInexistenteDa404YNoGuardaNada() {
        when(repo.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> servicio.editar(99L, req, "admin@correo.com"));

        verify(repo, never()).agregarVersion(anyLong(), any());
    }
}
