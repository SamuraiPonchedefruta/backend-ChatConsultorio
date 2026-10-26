package com.autobot.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.autobot.backend.auth.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Autenticación con JWT de punta a punta: filtro real, Spring Security y PostgreSQL efímero.
 * El secreto y el emisor son los de la configuración real (JWT_SECRET del entorno o del .env).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JwtIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String secreto;

    @Value("${app.jwt.issuer}")
    private String emisor;

    /** Crea la cuenta por el endpoint real, para que el correo del token exista en la base. */
    private void registrar(String correo, String celular) throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Perez","correo":"%s","celular":"%s","password":"ClaveSegura123",
                                 "consentimientoDatos":{"aceptado":true,"version":"1.0"}}
                                """.formatted(correo, celular)))
                .andExpect(status().isCreated());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Token firmado con la clave indicada, igual que los reales pero con la emisión y la vida a elección. */
    private String tokenFirmado(String claveSecreta, Instant emitidoEn, Duration vida, String correo) {
        SecretKey clave = new SecretKeySpec(claveSecreta.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(correo)
                .issuedAt(emitidoEn)
                .expiresAt(emitidoEn.plus(vida))
                .claim("roles", List.of("CLIENTE"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    @DisplayName("Un token válido autentica al usuario: el endpoint protegido responde con sus datos")
    void tokenValidoAutenticaAlUsuario() throws Exception {
        String correo = "ana.jwt@example.com";
        registrar(correo, "61232001");
        String token = jwtService.generarToken(correo, List.of("CLIENTE"));

        mockMvc.perform(get("/consentimientos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("datos"))
                .andExpect(jsonPath("$[0].estado").value("ACEPTADO"));
    }

    @Test
    @DisplayName("Un token expirado responde 401")
    void tokenExpiradoResponde401() throws Exception {
        String expirado = tokenFirmado(secreto, Instant.now().minus(Duration.ofHours(2)),
                Duration.ofHours(1), "ana.jwt@example.com");

        mockMvc.perform(get("/consentimientos").header(HttpHeaders.AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token alterado (rol cambiado a ADMIN) responde 401")
    void tokenAlteradoResponde401() throws Exception {
        String token = jwtService.generarToken("ana.jwt@example.com", List.of("CLIENTE"));
        String[] partes = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("CLIENTE", "ADMIN");
        String forjado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        mockMvc.perform(get("/consentimientos").header(HttpHeaders.AUTHORIZATION, bearer(forjado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token firmado con otra clave responde 401")
    void tokenDeOtraClaveResponde401() throws Exception {
        String ajeno = tokenFirmado("una-clave-ajena-que-tampoco-es-la-del-servidor-1234567890",
                Instant.now(), Duration.ofHours(1), "ana.jwt@example.com");

        mockMvc.perform(get("/consentimientos").header(HttpHeaders.AUTHORIZATION, bearer(ajeno)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sin token, o con otro esquema de autenticación, responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/consentimientos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/consentimientos").header(HttpHeaders.AUTHORIZATION, "Basic YW5hOmNsYXZl"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token malo no cierra las rutas públicas: el registro y el health siguen abiertos")
    void tokenMaloNoAfectaLasRutasPublicas() throws Exception {
        String expirado = tokenFirmado(secreto, Instant.now().minus(Duration.ofHours(2)),
                Duration.ofHours(1), "ana.jwt@example.com");

        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expirado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Rosa Mena","correo":"rosa.publica@example.com","celular":"61232002",
                                 "password":"ClaveSegura123",
                                 "consentimientoDatos":{"aceptado":true,"version":"1.0"}}
                                """))
                .andExpect(status().isCreated());
    }
}
