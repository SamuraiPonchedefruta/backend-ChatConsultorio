package com.autobot.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.autobot.backend.auth.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Genera y valida tokens JWT firmados con HS256.
 *
 * - sub: correo del usuario (así Principal.getName() es el correo).
 * - roles: nombres de rol sin prefijo (por ejemplo CLIENTE).
 * - iss, iat y exp: emisor, emisión y expiración. Un token sin expiración se rechaza.
 *
 * La expiración se valida sin tolerancia de reloj: un token vencido es un token vencido.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final String emisor;
    private final Duration vida;
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    public JwtService(JwtProperties propiedades) {
        String secreto = propiedades.secret();
        if (secreto == null || secreto.isBlank() || secreto.startsWith("${")) {
            throw new IllegalStateException("Falta la variable de entorno JWT_SECRET.");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes. Genera uno con: openssl rand -base64 48");
        }
        if (propiedades.expirationMinutes() <= 0) {
            throw new IllegalStateException("app.jwt.expiration-minutes debe ser mayor que cero.");
        }
        if (propiedades.issuer() == null || propiedades.issuer().isBlank()) {
            throw new IllegalStateException("app.jwt.issuer no puede estar vacío.");
        }

        this.emisor = propiedades.issuer();
        this.vida = Duration.ofMinutes(propiedades.expirationMinutes());

        SecretKey clave = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));

        // Solo acepta HS256: los tokens sin firma (alg none) y otros algoritmos se rechazan.
        NimbusJwtDecoder nuevoDecoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        nuevoDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(this.emisor),
                exigeExpiracionYSujeto()));
        this.decoder = nuevoDecoder;
    }

    /** Genera un token para el usuario con la expiración configurada. */
    public String generarToken(String correo, Collection<String> roles) {
        return generar(correo, roles, Instant.now());
    }

    /** Igual que generarToken pero con el instante de emisión explícito; permite probar tokens vencidos. */
    String generar(String correo, Collection<String> roles, Instant emitidoEn) {
        Objects.requireNonNull(correo, "correo");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(correo)
                .issuedAt(emitidoEn)
                .expiresAt(emitidoEn.plus(vida))
                .claim("roles", List.copyOf(roles))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    /**
     * Valida firma, emisor y expiración.
     *
     * @return los datos del token, o vacío si es inválido por cualquier motivo
     *         (vencido, alterado, de otro emisor, sin firma, mal formado...)
     */
    public Optional<DatosToken> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = decoder.decode(token);
            List<String> roles = jwt.getClaimAsStringList("roles");
            return Optional.of(new DatosToken(
                    jwt.getSubject(), roles == null ? List.of() : roles, jwt.getExpiresAt()));
        } catch (JwtException ex) {
            // Nunca se registra el token: solo el motivo del rechazo.
            log.debug("Token JWT rechazado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static OAuth2TokenValidator<Jwt> exigeExpiracionYSujeto() {
        return jwt -> {
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "El token debe tener expiración y sujeto", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    /** Datos de un token válido. */
    public record DatosToken(String correo, List<String> roles, Instant expiraEn) {
    }
}
