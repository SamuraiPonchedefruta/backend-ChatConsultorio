package com.autobot.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.model.ResetToken;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.ResetTokenRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

/**
 * Ciclo de vida del token de restablecimiento de contraseña: se genera, vence y se usa una sola vez.
 *
 * - El token son 32 bytes aleatorios de SecureRandom, en base64 URL. No es un JWT.
 * - En la base de datos solo se guarda su hash SHA-256: el token en claro se devuelve una única vez,
 *   a quien lo va a entregar a la persona, y no se puede recuperar después.
 * - Un token nuevo invalida los anteriores que el usuario no haya usado.
 * - Vencido, inexistente y reutilizado se rechazan con la misma excepción.
 *
 * Este servicio no cambia contraseñas ni envía correos: devuelve a quién pertenece el token y quien lo
 * llama decide qué hacer.
 */
@Service
public class ResetPasswordService {

    private static final int BYTES_DEL_TOKEN = 32;

    private final ResetTokenRepository tokens;
    private final UsuarioRepository usuarios;
    private final Duration vigencia;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public ResetPasswordService(
            ResetTokenRepository tokens,
            UsuarioRepository usuarios,
            @Value("${app.reset.expiracion-minutos:30}") long expiracionMinutos) {
        this(tokens, usuarios, Duration.ofMinutes(expiracionMinutos), Clock.systemUTC());
    }

    /** Permite inyectar el reloj en las pruebas para comprobar el vencimiento sin esperar. */
    ResetPasswordService(ResetTokenRepository tokens, UsuarioRepository usuarios, Duration vigencia, Clock reloj) {
        if (vigencia.isZero() || vigencia.isNegative()) {
            throw new IllegalStateException("app.reset.expiracion-minutos debe ser mayor que cero.");
        }
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.vigencia = vigencia;
        this.reloj = reloj;
    }

    /**
     * Genera un token nuevo para el usuario con ese correo e invalida los que tuviera sin usar.
     *
     * @return el token y su vencimiento, o vacío si no existe una cuenta con ese correo; así quien llame
     *         puede responder igual exista o no la cuenta
     */
    @Transactional
    public Optional<TokenGenerado> generarToken(String correo) {
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> usuario = usuarios.findByCorreo(correo.trim().toLowerCase(Locale.ROOT));
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        Instant ahora = reloj.instant();
        tokens.invalidarPendientes(usuario.get().getId(), ahora);

        String token = nuevoTokenAleatorio();
        Instant expiraEn = ahora.plus(vigencia);
        tokens.saveAndFlush(new ResetToken(usuario.get(), hash(token), expiraEn, ahora));

        return Optional.of(new TokenGenerado(token, expiraEn));
    }

    /**
     * Valida y consume el token. Funciona una sola vez.
     *
     * @return el id del usuario al que pertenece el token
     * @throws TokenResetInvalidoException si el token está vencido, ya se usó o no existe
     */
    @Transactional
    public Long consumirToken(String token) {
        if (token == null || token.isBlank()) {
            throw new TokenResetInvalidoException();
        }
        String hash = hash(token.trim());

        ResetToken guardado = tokens.findByTokenHash(hash).orElseThrow(TokenResetInvalidoException::new);

        // La sentencia es atómica: si otra petición consumió el token entre la lectura y este punto,
        // devuelve 0 y esta petición se rechaza.
        if (tokens.consumir(hash, reloj.instant()) != 1) {
            throw new TokenResetInvalidoException();
        }
        return guardado.getUsuario().getId();
    }

    private String nuevoTokenAleatorio() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal minúscula (64 caracteres). */
    static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", ex);
        }
    }

    /** Token en claro y su vencimiento. El token no se puede volver a obtener. */
    public record TokenGenerado(String token, Instant expiraEn) {

        /** Evita que el token termine en los logs si alguien imprime el objeto. */
        @Override
        public String toString() {
            return "TokenGenerado[expiraEn=" + expiraEn + "]";
        }
    }
}
