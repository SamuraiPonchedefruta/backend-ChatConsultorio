package com.autobot.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.autobot.backend.auth.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public record TokenEmitido(String token, Instant expiraEn) {}

    public record DatosToken(String correo, List<String> roles, Instant expiraEn, String jti) {
        public DatosToken(String correo, List<String> roles, Instant expiraEn) {
            this(correo, roles, expiraEn, null);
        }
    }

    public JwtService(JwtProperties properties) {
        if (properties == null || properties.secret() == null || properties.secret().equals("${JWT_SECRET}")) {
            throw new IllegalStateException("JWT_SECRET no configurado");
        }
        if (properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 bytes");
        }
        if (properties.expirationMinutes() <= 0) {
            throw new IllegalStateException("expiration-minutes debe ser mayor que 0");
        }

        this.properties = properties;

        SecretKey key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public TokenEmitido emitir(String correo, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plusSeconds(properties.expirationMinutes() * 60);
        String token = generar(correo, roles, expiraEn);
        return new TokenEmitido(token, expiraEn);
    }

    public String generarToken(String correo, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plusSeconds(properties.expirationMinutes() * 60);
        return generar(correo, roles, expiraEn);
    }

    public String generar(String correo, List<String> roles, Instant expiraEn) {
        Instant ahora = Instant.now();
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(correo)
                .id(jti)
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .claim("roles", roles)
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

public Optional<DatosToken> validar(String token) {
    if (token == null || token.isBlank()) {
        return Optional.empty();
    }
    try {
        Jwt jwt = decoder.decode(token);

        if (properties.issuer() != null && !properties.issuer().equals(jwt.getClaimAsString("iss"))) {
            return Optional.empty();
        }

        String correo = jwt.getSubject();
        Instant expiraEn = jwt.getExpiresAt();
        if (correo == null || expiraEn == null || expiraEn.isBefore(Instant.now())) {
            return Optional.empty();
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        return Optional.of(new DatosToken(correo, roles != null ? roles : List.of(), expiraEn, jwt.getId()));
    } catch (Exception e) {
        log.debug("Token rechazado: {}", e.getClass().getSimpleName());
        return Optional.empty();
    }
}
}