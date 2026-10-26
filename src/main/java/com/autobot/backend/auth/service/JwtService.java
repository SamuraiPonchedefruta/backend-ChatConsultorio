package com.autobot.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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

    public Optional<DatosToken> validar(String token) {
        if (token == null || token.isBlank()) {
            System.out.println("VAL_DEBUG: Token nulo o en blanco");
            return Optional.empty();
        }
        try {
            System.out.println("VAL_DEBUG: Intentando decodificar token...");
            Jwt jwt = decoder.decode(token);
            System.out.println("VAL_DEBUG: Token decodificado correctamente.");

            String issuerReal = jwt.getClaimAsString("iss");
            System.out.println("VAL_DEBUG: Issuer esperado=" + properties.issuer() + ", real=" + issuerReal);
            if (properties.issuer() != null && !properties.issuer().equals(issuerReal)) {
                System.out.println("VAL_DEBUG: ¡El issuer no coincide!");
                return Optional.empty();
            }

            String correo = jwt.getSubject();
            System.out.println("VAL_DEBUG: Subject (correo)=" + correo);

            List<String> roles = jwt.getClaimAsStringList("roles");
            System.out.println("VAL_DEBUG: Roles=" + roles);

            Instant expiraEn = jwt.getExpiresAt();
            System.out.println("VAL_DEBUG: ExpiraEn=" + expiraEn + ", Ahora=" + Instant.now());

            if (correo == null || expiraEn == null || expiraEn.isBefore(Instant.now())) {
                System.out.println("VAL_DEBUG: ¡Correo nulo, expiraEn nulo o token expirado!");
                return Optional.empty();
            }

            System.out.println("VAL_DEBUG: ¡Token validado con éxito!");
            return Optional.of(new DatosToken(correo, roles != null ? roles : List.of(), expiraEn, jwt.getId()));
        } catch (Exception e) {
            System.err.println("VAL_DEBUG CATCH EXCEPTION: " + e.getClass().getName() + " -> " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}