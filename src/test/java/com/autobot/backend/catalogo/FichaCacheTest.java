package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.auth.repository.BitacoraRepository;
import com.autobot.backend.auth.service.JwtService;
import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.common.config.CacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Usa un servidor real (no MockMvc) y no es transaccional: la caché espera al commit,
 * y el gzip lo aplica el servidor. Por eso limpia sus propios datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaCacheTest extends AbstractIntegrationTest {

    @LocalServerPort
    int puerto;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    BitacoraRepository bitacora;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();
    private Modelo modelo;

    @BeforeEach
    void setUp() {
        limpiar();
        Modelo nuevo = new Modelo("Toyota", "Hilux");
        nuevo.asignarFicha(new FichaModelo(
                "9 l/100 km", "Aceite cada 10000 km", "Embrague", "Filtro de aire", "Suspension"));
        modelo = modelos.save(nuevo);
    }

    @AfterEach
    void limpiar() {
        bitacora.deleteAll();
        modelos.deleteAll();
        cache().clear();
    }

    private Cache cache() {
        return cacheManager.getCache(CacheConfig.FICHAS);
    }

    private String base() {
        return "http://localhost:" + puerto;
    }

    private HttpResponse<byte[]> consultar(Long id, String... cabecera) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base() + "/modelos/" + id)).GET();
        if (cabecera.length == 2) {
            peticion.header(cabecera[0], cabecera[1]);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private String consumoDe(HttpResponse<byte[]> respuesta) throws Exception {
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return mapper.readTree(respuesta.body()).get("consumo").asText();
    }

    private HttpResponse<byte[]> editarComoAdmin(Long id, String consumoNuevo) throws Exception {
        String token = jwtService.generarToken("admin@example.com", List.of("ADMIN"));
        String json = """
                {"marca":"Toyota","nombre":"Hilux","consumo":"%s",
                 "mantenimiento":"Aceite cada 10000 km","fallasComunes":"Embrague",
                 "repuestos":"Filtro de aire","queRevisar":"Suspension"}""".formatted(consumoNuevo);
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base() + "/admin/modelos/" + id))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void laSegundaConsultaSaleDeCache() throws Exception {
        assertThat(cache().get(modelo.getId())).isNull();

        assertThat(consumoDe(consultar(modelo.getId()))).isEqualTo("9 l/100 km");
        assertThat(cache().get(modelo.getId())).isNotNull();

        // Se cambia el dato directo en la base, sin pasar por el servicio. Si la respuesta
        // sigue igual, es porque salió de la caché y no de la base.
        jdbc.update("UPDATE ficha_modelo SET consumo = ? WHERE id_modelo = ?",
                "CAMBIADO EN LA BD", modelo.getId());

        assertThat(consumoDe(consultar(modelo.getId()))).isEqualTo("9 l/100 km");
    }

    @Test
    void laEdicionDelAdministradorInvalidaLaCache() throws Exception {
        assertThat(consumoDe(consultar(modelo.getId()))).isEqualTo("9 l/100 km");
        assertThat(cache().get(modelo.getId())).isNotNull();

        assertThat(editarComoAdmin(modelo.getId(), "7 l/100 km").statusCode()).isEqualTo(200);

        assertThat(cache().get(modelo.getId())).isNull();
        assertThat(consumoDe(consultar(modelo.getId()))).isEqualTo("7 l/100 km");
        assertThat(cache().get(modelo.getId())).isNotNull();
    }

    @Test
    void unModeloInexistenteNoSeGuardaEnCache() throws Exception {
        assertThat(consultar(999999L).statusCode()).isEqualTo(404);

        assertThat(cache().get(999999L)).isNull();
    }

    @Test
    void laRespuestaSeComprimeSiElClienteLoAcepta() throws Exception {
        jdbc.update("UPDATE ficha_modelo SET consumo = ? WHERE id_modelo = ?",
                "x".repeat(3000), modelo.getId());

        HttpResponse<byte[]> respuesta = consultar(modelo.getId(), "Accept-Encoding", "gzip");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Content-Encoding")).contains("gzip");
        assertThat(respuesta.body().length).isLessThan(1000);
    }

    @Test
    void sinAcceptEncodingLaRespuestaNoSeComprime() throws Exception {
        jdbc.update("UPDATE ficha_modelo SET consumo = ? WHERE id_modelo = ?",
                "x".repeat(3000), modelo.getId());

        HttpResponse<byte[]> respuesta = consultar(modelo.getId());

        assertThat(respuesta.headers().firstValue("Content-Encoding")).isEmpty();
        assertThat(respuesta.body().length).isGreaterThan(3000);
    }
}
