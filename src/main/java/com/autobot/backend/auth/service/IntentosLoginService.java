package com.autobot.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Limita los intentos fallidos consecutivos de inicio de sesión.
 *
 * - Al llegar a max-intentos fallos seguidos sobre la misma clave, esa clave queda bloqueada durante
 *   bloqueo-minutos. El fallo que alcanza el límite ya responde con el bloqueo.
 * - Durante el bloqueo no se verifica la contraseña, ni siquiera si es la correcta.
 * - Un login exitoso reinicia el contador. El bloqueo se libera solo cuando vence.
 * - Los fallos se olvidan si pasa el mismo tiempo del bloqueo sin nuevos fallos.
 *
 * La clave la decide quien llama (AuthService usa la cuenta si existe y el identificador escrito si no),
 * así que una cuenta inexistente se bloquea igual que una real y la respuesta no delata cuáles existen.
 *
 * Los contadores viven en memoria: se pierden al reiniciar y no se comparten entre instancias.
 */
@Service
public class IntentosLoginService {

    private static final Duration INTERVALO_LIMPIEZA = Duration.ofMinutes(1);

    private final int maxIntentos;
    private final Duration bloqueo;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> proximaLimpieza;

    @Autowired
    public IntentosLoginService(
            @Value("${app.login.max-intentos}") int maxIntentos,
            @Value("${app.login.bloqueo-minutos}") long bloqueoMinutos) {
        this(maxIntentos, Duration.ofMinutes(bloqueoMinutos), Clock.systemUTC());
    }

    /** Permite inyectar el reloj en las pruebas para comprobar el vencimiento sin esperar. */
    IntentosLoginService(int maxIntentos, Duration bloqueo, Clock reloj) {
        if (maxIntentos < 1) {
            throw new IllegalStateException("app.login.max-intentos debe ser al menos 1.");
        }
        if (bloqueo.isZero() || bloqueo.isNegative()) {
            throw new IllegalStateException("app.login.bloqueo-minutos debe ser mayor que cero.");
        }
        this.maxIntentos = maxIntentos;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
        this.proximaLimpieza = new AtomicReference<>(reloj.instant().plus(INTERVALO_LIMPIEZA));
    }

    /**
     * Debe llamarse antes de verificar la contraseña.
     *
     * @throws CuentaBloqueadaException si la clave está bloqueada y el bloqueo no ha vencido
     */
    public void verificarNoBloqueado(String clave) {
        Instant ahora = reloj.instant();
        Registro registro = registros.get(clave);
        if (registro == null || registro.bloqueadoHasta() == null) {
            return;
        }
        if (ahora.isBefore(registro.bloqueadoHasta())) {
            throw new CuentaBloqueadaException(Duration.between(ahora, registro.bloqueadoHasta()));
        }
        // El bloqueo venció: se libera y se empieza de cero.
        registros.remove(clave, registro);
    }

    /**
     * Registra un intento fallido.
     *
     * @throws CuentaBloqueadaException si este fallo alcanza el límite y deja la clave bloqueada
     */
    public void registrarFallo(String clave) {
        Instant ahora = reloj.instant();
        limpiarSiCorresponde(ahora);

        Registro nuevo = registros.compute(clave, (k, actual) -> siguiente(actual, ahora));

        if (nuevo.bloqueadoHasta() != null && ahora.isBefore(nuevo.bloqueadoHasta())) {
            throw new CuentaBloqueadaException(Duration.between(ahora, nuevo.bloqueadoHasta()));
        }
    }

    /** Un login exitoso reinicia el contador de esa clave. */
    public void registrarExito(String clave) {
        registros.remove(clave);
    }

    private Registro siguiente(Registro actual, Instant ahora) {
        if (actual != null && actual.bloqueadoHasta() != null && ahora.isBefore(actual.bloqueadoHasta())) {
            return actual; // ya está bloqueada: un fallo más no extiende el bloqueo
        }
        int fallos = (actual == null || estaVencido(actual, ahora)) ? 1 : actual.fallos() + 1;
        Instant bloqueadoHasta = fallos >= maxIntentos ? ahora.plus(bloqueo) : null;
        return new Registro(fallos, bloqueadoHasta, ahora);
    }

    private boolean estaVencido(Registro registro, Instant ahora) {
        Instant limite = registro.bloqueadoHasta() != null
                ? registro.bloqueadoHasta()
                : registro.ultimoFallo().plus(bloqueo);
        return !ahora.isBefore(limite);
    }

    /** Borra los registros vencidos, como mucho una vez por minuto, para que la memoria no crezca sin límite. */
    private void limpiarSiCorresponde(Instant ahora) {
        Instant toca = proximaLimpieza.get();
        if (ahora.isBefore(toca)) {
            return;
        }
        if (proximaLimpieza.compareAndSet(toca, ahora.plus(INTERVALO_LIMPIEZA))) {
            registros.values().removeIf(registro -> estaVencido(registro, ahora));
        }
    }

    /** Cantidad de claves con estado guardado; solo para las pruebas. */
    int registrosActivos() {
        return registros.size();
    }

    private record Registro(int fallos, Instant bloqueadoHasta, Instant ultimoFallo) {
    }
}
