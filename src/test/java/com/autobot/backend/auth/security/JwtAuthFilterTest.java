package com.autobot.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.autobot.backend.auth.config.JwtProperties;
import com.autobot.backend.auth.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.FilterChain;

/** Pruebas del filtro sin levantar Spring: ejercitan la lectura del encabezado y el contexto de seguridad. */
class JwtAuthFilterTest {

    private static final String SECRETO = "clave-de-prueba-solo-para-tests-que-supera-los-32-bytes";
    private static final String CORREO = "ana@example.com";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRETO, 60, "autobot-backend"));
    private final JwtAuthFilter filtro = new JwtAuthFilter(jwtService);

    private boolean cadenaEjecutada;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    /** Token bien firmado con la clave correcta pero emitido hace 3 horas, con vida de 60 minutos. */
    private static String tokenVencido() {
        SecretKey clave = new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        Instant emitidoEn = Instant.now().minus(Duration.ofHours(3));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("autobot-backend")
                .subject(CORREO)
                .issuedAt(emitidoEn)
                .expiresAt(emitidoEn.plus(Duration.ofMinutes(60)))
                .claim("roles", List.of("CLIENTE"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** Ejecuta el filtro y devuelve la autenticación que vería el resto de la cadena. */
    private Authentication ejecutar(String cabeceraAuthorization) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/consentimientos");
        if (cabeceraAuthorization != null) {
            peticion.addHeader(HttpHeaders.AUTHORIZATION, cabeceraAuthorization);
        }
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        Authentication[] vista = new Authentication[1];
        FilterChain cadena = (req, res) -> {
            cadenaEjecutada = true;
            vista[0] = SecurityContextHolder.getContext().getAuthentication();
        };

        filtro.doFilter(peticion, respuesta, cadena);

        assertThat(respuesta.getStatus()).as("el filtro nunca responde por su cuenta").isEqualTo(200);
        return vista[0];
    }

    @Test
    @DisplayName("Un token válido deja autenticado al usuario con su correo y sus roles")
    void tokenValidoAutentica() throws Exception {
        String token = jwtService.generarToken(CORREO, List.of("CLIENTE", "ADMIN"));

        Authentication autenticacion = ejecutar("Bearer " + token);

        assertThat(cadenaEjecutada).isTrue();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.isAuthenticated()).isTrue();
        assertThat(autenticacion.getName()).isEqualTo(CORREO);
        assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CLIENTE", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("El esquema Bearer no distingue mayúsculas")
    void esquemaBearerSinDistinguirMayusculas() throws Exception {
        String token = jwtService.generarToken(CORREO, List.of("CLIENTE"));

        assertThat(ejecutar("bearer " + token)).isNotNull();
    }

    @Test
    @DisplayName("Sin encabezado Authorization la petición sigue sin autenticar")
    void sinEncabezadoNoAutentica() throws Exception {
        assertThat(ejecutar(null)).isNull();
        assertThat(cadenaEjecutada).isTrue();
    }

    @Test
    @DisplayName("Otro esquema (Basic) se ignora")
    void otroEsquemaSeIgnora() throws Exception {
        assertThat(ejecutar("Basic YW5hOmNsYXZl")).isNull();
        assertThat(cadenaEjecutada).isTrue();
    }

    @Test
    @DisplayName("Un token vencido o inválido no autentica, pero la cadena continúa para que responda 401")
    void tokenInvalidoNoAutenticaYContinua() throws Exception {
        String vencido = tokenVencido();

        assertThat(ejecutar("Bearer " + vencido)).isNull();
        assertThat(cadenaEjecutada).isTrue();

        cadenaEjecutada = false;
        assertThat(ejecutar("Bearer esto-no-es-un-token")).isNull();
        assertThat(cadenaEjecutada).isTrue();
    }
}
