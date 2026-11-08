package com.autobot.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.AbstractIntegrationTest;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.repository.EmbeddingRepository;
import com.autobot.backend.catalogo.repository.ModeloRepository;

@SpringBootTest
@Transactional
class EmbeddingRepositoryTest extends AbstractIntegrationTest {

    /** Debe coincidir con vector(384) de V13__embeddings.sql. */
    private static final int DIMENSION = 384;

    @Autowired
    EmbeddingRepository embeddings;

    @Autowired
    ModeloRepository modelos;

    @Autowired
    JdbcTemplate jdbc;

    /** Vector de longitud 1 que apunta solo en la posición dada (cómodo para razonar distancias). */
    private static float[] unitario(int posicion) {
        float[] v = new float[DIMENSION];
        v[posicion] = 1f;
        return v;
    }

    private Modelo guardarModelo(String nombre) {
        return modelos.saveAndFlush(new Modelo("Toyota", nombre));
    }

    @Test
    void guardaUnVectorConLaDimensionConfigurada() {
        Modelo hilux = guardarModelo("Hilux");

        embeddings.guardar(hilux.getId(), "ficha:consumo", "9 l/100 km", unitario(0));

        assertThat(embeddings.findAll()).hasSize(1);
        Integer dimension = jdbc.queryForObject(
                "SELECT vector_dims(embedding) FROM modelo_embedding", Integer.class);
        assertThat(dimension).isEqualTo(DIMENSION);
    }

    @Test
    void laBusquedaPorSimilitudDevuelveElMasCercanoPrimero() {
        Modelo hilux = guardarModelo("Hilux");
        Modelo corolla = guardarModelo("Corolla");
        Modelo yaris = guardarModelo("Yaris");
        embeddings.guardar(hilux.getId(), "ficha:consumo", "texto hilux", unitario(0));
        embeddings.guardar(corolla.getId(), "ficha:consumo", "texto corolla", unitario(1));
        embeddings.guardar(yaris.getId(), "ficha:consumo", "texto yaris", unitario(2));

        List<EmbeddingRepository.Similar> resultados = embeddings.buscar(unitario(0), 2);

        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).getIdModelo()).isEqualTo(hilux.getId());
        assertThat(resultados.get(0).getContenido()).isEqualTo("texto hilux");
        assertThat(resultados.get(0).getDistancia()).isCloseTo(0.0, within(1e-6));
        assertThat(resultados.get(1).getDistancia()).isGreaterThan(resultados.get(0).getDistancia());
    }

    @Test
    void guardarDeNuevoLaMismaFuenteReemplazaElVector() {
        Modelo hilux = guardarModelo("Hilux");
        embeddings.guardar(hilux.getId(), "ficha:consumo", "texto viejo", unitario(0));

        embeddings.guardar(hilux.getId(), "ficha:consumo", "texto nuevo", unitario(1));

        assertThat(embeddings.findAll()).hasSize(1);
        assertThat(embeddings.findAll().get(0).getContenido()).isEqualTo("texto nuevo");
        assertThat(embeddings.buscar(unitario(1), 1).get(0).getDistancia())
                .isCloseTo(0.0, within(1e-6));
    }

    @Test
    void rechazaUnVectorConDimensionIncorrecta() {
        Modelo hilux = guardarModelo("Hilux");

        assertThatThrownBy(() -> embeddings.guardar(
                hilux.getId(), "ficha:consumo", "texto", new float[] { 1f, 2f, 3f }))
                .isInstanceOf(DataAccessException.class);
    }
}
