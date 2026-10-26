package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.autobot.backend.auth.config.JwtProperties;
import com.autobot.backend.auth.service.JwtService.DatosToken;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Pruebas unitarias, sin Spring ni base de datos.
 * Las claves de este archivo son valores de prueba que no se usan en ningún entorno.
 */
class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-solo-para-tests-que-supera-los-32-bytes";
    private static final String OTRO_SECRETO = "otra-clave-de-prueba-distinta-que-tambien-supera-32-bytes";
    private static final String EMISOR = "autobot-backend";
    private static final String CORREO = "ana@example.com";

    private final JwtService servicio = crearServicio(SECRETO, EMISOR);

    private static JwtService crearServicio(String secreto, String emisor) {
        return new JwtService(new JwtProperties(secreto, 60, emisor));
    }

    private static String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Un token recién generado es válido y trae el correo, los roles y la expiración")
    void tokenValidoSeAutentica() {
        Instant antes = Instant.now();

        String token = servicio.generarToken(CORREO, List.of("CLIENTE"));
        Optional<DatosToken> datos = servicio.validar(token);

        assertThat(datos).hasValueSatisfying(d -> {
            assertThat(d.correo()).isEqualTo(CORREO);
            assertThat(d.roles()).containsExactly("CLIENTE");
            assertThat(d.expiraEn()).isBetween(
                    antes.plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));
        });
    }

    @Test
    @DisplayName("Un token vencido se rechaza, incluso por pocos segundos")
    void tokenVencidoSeRechaza() {
        String vencidoHaceSegundos = servicio.generar(
                CORREO, List.of("CLIENTE"), Instant.now().minus(Duration.ofMinutes(60)).minusSeconds(5));
        String vencidoHaceHoras = servicio.generar(
                CORREO, List.of("CLIENTE"), Instant.now().minus(Duration.ofHours(5)));

        assertThat(servicio.validar(vencidoHaceSegundos)).isEmpty();
        assertThat(servicio.validar(vencidoHaceHoras)).isEmpty();
    }

    @Test
    @DisplayName("Un token que aún no vence sigue siendo válido (el rechazo no es demasiado estricto)")
    void tokenPorVencerSigueValido() {
        String casiVencido = servicio.generar(
                CORREO, List.of("CLIENTE"), Instant.now().minus(Duration.ofMinutes(59)));

        assertThat(servicio.validar(casiVencido)).isPresent();
    }

    @Test
    @DisplayName("Un token con el contenido alterado (cambiar el rol a ADMIN) se rechaza")
    void tokenAlteradoSeRechaza() {
        String token = servicio.generarToken(CORREO, List.of("CLIENTE"));
        String[] partes = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("CLIENTE");

        String payloadForjado = payload.replace("CLIENTE", "ADMIN");
        String forjado = partes[0] + "." + base64Url(payloadForjado) + "." + partes[2];

        assertThat(forjado).isNotEqualTo(token);
        assertThat(servicio.validar(forjado)).isEmpty();
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void tokenDeOtraClaveSeRechaza() {
        String token = crearServicio(OTRO_SECRETO, EMISOR).generarToken(CORREO, List.of("CLIENTE"));

        assertThat(servicio.validar(token)).isEmpty();
    }

    @Test
    @DisplayName("Un token de otro emisor se rechaza aunque la firma sea correcta")
    void tokenDeOtroEmisorSeRechaza() {
        String token = crearServicio(SECRETO, "otra-aplicacion").generarToken(CORREO, List.of("CLIENTE"));

        assertThat(servicio.validar(token)).isEmpty();
    }

    @Test
    @DisplayName("Un token sin firma (alg none) se rechaza")
    void tokenSinFirmaSeRechaza() {
        String cabecera = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"iss\":\"" + EMISOR + "\",\"sub\":\"" + CORREO + "\",\"roles\":[\"ADMIN\"],"
                + "\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}");

        assertThat(servicio.validar(cabecera + "." + payload + ".")).isEmpty();
    }

    @Test
    @DisplayName("Un token bien firmado pero sin expiración se rechaza")
    void tokenSinExpiracionSeRechaza() {
        SecretKey clave = new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(CORREO)
                .issuedAt(Instant.now())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThat(servicio.validar(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "a.b.c", "....", "Bearer abc", "eyJhbGciOiJIUzI1NiJ9.e30."})
    @DisplayName("Los valores que no son un JWT se rechazan sin lanzar excepciones")
    void valoresMalFormadosSeRechazan(String token) {
        assertThat(servicio.validar(token)).isEmpty();
    }

    @Test
    @DisplayName("No arranca si el secreto es demasiado corto")
    void secretoCortoNoArranca() {
        assertThatThrownBy(() -> crearServicio("corto", EMISOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("No arranca si la variable JWT_SECRET no se resolvió")
    void secretoSinResolverNoArranca() {
        assertThatThrownBy(() -> crearServicio("${JWT_SECRET}", EMISOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> crearServicio(null, EMISOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("No arranca con una expiración que no sea positiva")
    void expiracionInvalidaNoArranca() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRETO, 0, EMISOR)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expiration-minutes");
    }
}
