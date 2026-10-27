package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas sin Spring ni base de datos. El reloj se adelanta a mano, así que comprobar un bloqueo de
 * 15 minutos no obliga a esperar 15 minutos.
 */
class IntentosLoginTest {

    private static final String CUENTA = "usuario:1";
    private static final String OTRA_CUENTA = "usuario:2";

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-27T12:00:00Z"));
    private final IntentosLoginService servicio = new IntentosLoginService(3, Duration.ofMinutes(15), reloj);

    @Test
    @DisplayName("Sin intentos fallidos no hay bloqueo")
    void sinFallosNoHayBloqueo() {
        assertThatCode(() -> servicio.verificarNoBloqueado(CUENTA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Los fallos por debajo del límite no bloquean")
    void porDebajoDelLimiteNoBloquea() {
        assertThatCode(() -> {
            servicio.registrarFallo(CUENTA);
            servicio.registrarFallo(CUENTA);
            servicio.verificarNoBloqueado(CUENTA);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("El fallo que alcanza el límite bloquea la cuenta y lo informa")
    void elUltimoFalloBloquea() {
        servicio.registrarFallo(CUENTA);
        servicio.registrarFallo(CUENTA);

        assertThatThrownBy(() -> servicio.registrarFallo(CUENTA))
                .isInstanceOfSatisfying(CuentaBloqueadaException.class, ex -> {
                    assertThat(ex.getSegundosRestantes()).isEqualTo(900);
                    assertThat(ex.getMessage()).contains("15 minutos");
                });
    }

    @Test
    @DisplayName("Durante el bloqueo las respuestas informan el tiempo que falta, que va bajando")
    void duranteElBloqueoSeInformaElTiempoRestante() {
        bloquear(CUENTA);

        reloj.avanzar(Duration.ofMinutes(5));

        assertThatThrownBy(() -> servicio.verificarNoBloqueado(CUENTA))
                .isInstanceOfSatisfying(CuentaBloqueadaException.class, ex -> {
                    assertThat(ex.getSegundosRestantes()).isEqualTo(600);
                    assertThat(ex.getMessage()).contains("10 minutos");
                });

        reloj.avanzar(Duration.ofMinutes(9).plusSeconds(30));

        assertThatThrownBy(() -> servicio.verificarNoBloqueado(CUENTA))
                .isInstanceOfSatisfying(CuentaBloqueadaException.class, ex -> {
                    assertThat(ex.getSegundosRestantes()).isEqualTo(30);
                    assertThat(ex.getMessage()).contains("30 segundos");
                });
    }

    @Test
    @DisplayName("El bloqueo se mantiene hasta el último instante y se libera al vencer")
    void elBloqueoSeLiberaAlVencer() {
        bloquear(CUENTA);

        reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));
        assertThatThrownBy(() -> servicio.verificarNoBloqueado(CUENTA))
                .isInstanceOf(CuentaBloqueadaException.class);

        reloj.avanzar(Duration.ofSeconds(1));
        assertThatCode(() -> servicio.verificarNoBloqueado(CUENTA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Tras liberarse, la cuenta vuelve a empezar de cero: hacen falta otros 3 fallos")
    void trasLaLiberacionElContadorEmpiezaDeCero() {
        bloquear(CUENTA);
        reloj.avanzar(Duration.ofMinutes(15));
        servicio.verificarNoBloqueado(CUENTA);

        assertThatCode(() -> {
            servicio.registrarFallo(CUENTA);
            servicio.registrarFallo(CUENTA);
        }).doesNotThrowAnyException();
        assertThatThrownBy(() -> servicio.registrarFallo(CUENTA)).isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("Un login exitoso reinicia el contador de fallos consecutivos")
    void unExitoReiniciaElContador() {
        servicio.registrarFallo(CUENTA);
        servicio.registrarFallo(CUENTA);

        servicio.registrarExito(CUENTA);

        assertThatCode(() -> {
            servicio.registrarFallo(CUENTA);
            servicio.registrarFallo(CUENTA);
        }).doesNotThrowAnyException();
        assertThatThrownBy(() -> servicio.registrarFallo(CUENTA)).isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("Cada cuenta lleva su propio contador: bloquear una no afecta a otra")
    void lasCuentasSonIndependientes() {
        bloquear(CUENTA);

        assertThatCode(() -> {
            servicio.verificarNoBloqueado(OTRA_CUENTA);
            servicio.registrarFallo(OTRA_CUENTA);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Los fallos se olvidan si pasa el tiempo del bloqueo sin nuevos fallos")
    void losFallosAntiguosSeOlvidan() {
        servicio.registrarFallo(CUENTA);
        servicio.registrarFallo(CUENTA);

        reloj.avanzar(Duration.ofMinutes(16));

        assertThatCode(() -> {
            servicio.registrarFallo(CUENTA);
            servicio.registrarFallo(CUENTA);
        }).doesNotThrowAnyException();
        assertThatThrownBy(() -> servicio.registrarFallo(CUENTA)).isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("Un fallo más durante el bloqueo no lo extiende")
    void unFalloDuranteElBloqueoNoLoExtiende() {
        bloquear(CUENTA);
        reloj.avanzar(Duration.ofMinutes(10));

        assertThatThrownBy(() -> servicio.registrarFallo(CUENTA)).isInstanceOf(CuentaBloqueadaException.class);

        reloj.avanzar(Duration.ofMinutes(5));
        assertThatCode(() -> servicio.verificarNoBloqueado(CUENTA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Los registros vencidos se limpian solos y la memoria no crece sin límite")
    void losRegistrosVencidosSeLimpian() {
        servicio.registrarFallo("identificador:a@example.com");
        servicio.registrarFallo("identificador:b@example.com");
        servicio.registrarFallo("identificador:c@example.com");
        assertThat(servicio.registrosActivos()).isEqualTo(3);

        reloj.avanzar(Duration.ofMinutes(20));
        servicio.registrarFallo("identificador:d@example.com");

        assertThat(servicio.registrosActivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una configuración inválida impide arrancar")
    void configuracionInvalida() {
        assertThatThrownBy(() -> new IntentosLoginService(0, Duration.ofMinutes(15), reloj))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-intentos");
        assertThatThrownBy(() -> new IntentosLoginService(3, Duration.ZERO, reloj))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueo-minutos");
    }

    /** Lleva la cuenta al bloqueo: los 3 fallos del límite configurado en esta prueba. */
    private void bloquear(String cuenta) {
        servicio.registrarFallo(cuenta);
        servicio.registrarFallo(cuenta);
        assertThatThrownBy(() -> servicio.registrarFallo(cuenta)).isInstanceOf(CuentaBloqueadaException.class);
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class RelojManual extends Clock {

        private Instant instante;

        RelojManual(Instant inicio) {
            this.instante = inicio;
        }

        void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
