package com.autobot.backend.catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.autobot.backend.catalogo.model.ModeloEmbedding;

public interface EmbeddingRepository extends JpaRepository<ModeloEmbedding, Long> {

    /** Resultado de una búsqueda por similitud (distancia coseno: 0 = idéntico). */
    interface Similar {
        Long getIdModelo();

        String getFuente();

        String getContenido();

        Double getDistancia();
    }

    /** Inserta el vector, o lo reemplaza si ya existe para ese modelo y esa fuente. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO modelo_embedding (id_modelo, fuente, contenido, embedding)
            VALUES (:idModelo, :fuente, :contenido, CAST(:embedding AS vector))
            ON CONFLICT (id_modelo, fuente) DO UPDATE
               SET contenido = EXCLUDED.contenido,
                   embedding = EXCLUDED.embedding,
                   creado_en = now()
            """, nativeQuery = true)
    void upsert(@Param("idModelo") Long idModelo,
            @Param("fuente") String fuente,
            @Param("contenido") String contenido,
            @Param("embedding") String embedding);

    /** Los {@code limite} textos más parecidos a la consulta, del más cercano al más lejano. */
    @Query(value = """
            SELECT id_modelo AS "idModelo",
                   fuente AS "fuente",
                   contenido AS "contenido",
                   embedding <=> CAST(:consulta AS vector) AS "distancia"
            FROM modelo_embedding
            ORDER BY embedding <=> CAST(:consulta AS vector)
            LIMIT :limite
            """, nativeQuery = true)
    List<Similar> buscarSimilares(@Param("consulta") String consulta,
            @Param("limite") int limite);

    default void guardar(Long idModelo, String fuente, String contenido, float[] embedding) {
        upsert(idModelo, fuente, contenido, comoLiteral(embedding));
    }

    default List<Similar> buscar(float[] consulta, int limite) {
        return buscarSimilares(comoLiteral(consulta), limite);
    }

    /** Convierte un arreglo al formato de texto de pgvector: {@code [0.1,0.2,0.3]}. */
    static String comoLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("El vector no puede estar vacío");
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) {
                throw new IllegalArgumentException("El vector contiene NaN o infinito");
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
