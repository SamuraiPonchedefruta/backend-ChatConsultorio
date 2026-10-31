package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.within;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autobot.backend.auth.model.Bitacora;
import com.autobot.backend.auth.model.Bitacora.Accion;
import com.autobot.backend.auth.repository.BitacoraRepository;

@ExtendWith(MockitoExtension.class)
class BitacoraServiceTest {

    @Mock
    BitacoraRepository repositorio;

    BitacoraService servicio;

    @BeforeEach
    void setUp() {
        servicio = new BitacoraService(repositorio);
        when(repositorio.save(any(Bitacora.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Bitacora capturar() {
        ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void altaRegistraAccionAdminYFecha() {
        servicio.registrarAlta("admin@example.com", "nuevo@example.com", "ADMIN");

        Bitacora registro = capturar();
        assertThat(registro.getAccion()).isEqualTo(Accion.ALTA);
        assertThat(registro.getCorreoAdmin()).isEqualTo("admin@example.com");
        assertThat(registro.getCorreoAfectado()).isEqualTo("nuevo@example.com");
        assertThat(registro.getDetalle()).contains("ADMIN");
        assertThat(registro.getFecha()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void desactivacionRegistraAccionYAdmin() {
        servicio.registrarDesactivacion("admin@example.com", "personal@example.com");

        Bitacora registro = capturar();
        assertThat(registro.getAccion()).isEqualTo(Accion.DESACTIVACION);
        assertThat(registro.getCorreoAdmin()).isEqualTo("admin@example.com");
        assertThat(registro.getCorreoAfectado()).isEqualTo("personal@example.com");
        assertThat(registro.getFecha()).isNotNull();
    }

    @Test
    void cambioDeRolGuardaRolAnteriorYNuevo() {
        servicio.registrarCambioRol("admin@example.com", "personal@example.com", "CLIENTE", "ADMIN");

        Bitacora registro = capturar();
        assertThat(registro.getAccion()).isEqualTo(Accion.CAMBIO_ROL);
        assertThat(registro.getDetalle()).contains("CLIENTE").contains("ADMIN");
        assertThat(registro.getFecha()).isNotNull();
    }

}
