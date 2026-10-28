package com.autobot.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.UsuarioRepository;
import com.autobot.backend.auth.service.ResetPasswordService;
import com.autobot.backend.auth.service.TokenResetInvalidoException;

/**
 * Con la aplicación completa y transacciones reales (que hacen commit), comprueba que "un solo uso" resiste
 * peticiones simultáneas: si varias llegan a la vez con el mismo token, solo una puede ganar.
 */
@SpringBootTest
@Testcontainers
class ResetTokenAtomicidadIntegracionTest {

    private static final int PETICIONES_SIMULTANEAS = 8;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ResetPasswordService servicio;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @DisplayName("Con el mismo token en paralelo, exactamente una petición lo consume y las demás se rechazan")
    void soloUnaPeticionConsumeElToken() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Ana Perez", "ana.concurrente@example.com", "hash"));
        String token = servicio.generarToken("ana.concurrente@example.com").orElseThrow().token();

        ExecutorService pool = Executors.newFixedThreadPool(PETICIONES_SIMULTANEAS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Long>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < PETICIONES_SIMULTANEAS; i++) {
                Callable<Long> peticion = () -> {
                    salida.await();
                    return servicio.consumirToken(token);
                };
                resultados.add(pool.submit(peticion));
            }
            salida.countDown();

            int aceptadas = 0;
            int rechazadas = 0;
            for (Future<Long> resultado : resultados) {
                try {
                    assertThat(resultado.get(30, TimeUnit.SECONDS)).isEqualTo(usuario.getId());
                    aceptadas++;
                } catch (java.util.concurrent.ExecutionException ex) {
                    assertThat(ex.getCause()).isInstanceOf(TokenResetInvalidoException.class);
                    rechazadas++;
                }
            }

            assertThat(aceptadas).isEqualTo(1);
            assertThat(rechazadas).isEqualTo(PETICIONES_SIMULTANEAS - 1);
        } finally {
            pool.shutdownNow();
        }
    }
}
