-- V13__embeddings.sql
-- E2-05-A: tabla de embeddings con pgvector y su índice.
--
-- DECISIÓN: modelo de embeddings y dimensión
--   Modelo:     intfloat/multilingual-e5-small (multilingüe, incluye español)
--   Dimensión:  384  (va fija en la columna: vector(384))
--   Métrica:    distancia coseno (operador <=>), índice HNSW con vector_cosine_ops
--   Uso E5:     anteponer "passage: " al texto al indexar y "query: " al buscar
--
-- Si algún día se cambia de modelo o de dimensión, NO se edita esta migración:
-- se crea una nueva que cambie el tipo de la columna y se recalculan todos
-- los vectores (los de modelos distintos no son comparables entre sí).

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE modelo_embedding (
    id_embedding BIGSERIAL PRIMARY KEY,
    id_modelo    BIGINT       NOT NULL REFERENCES modelo (id_modelo) ON DELETE CASCADE,
    fuente       VARCHAR(60)  NOT NULL,          -- de dónde sale el texto, p. ej. 'ficha:consumo'
    contenido    TEXT         NOT NULL,          -- texto que se convirtió en vector
    embedding    vector(384)  NOT NULL,
    modelo_ia    VARCHAR(100) NOT NULL DEFAULT 'intfloat/multilingual-e5-small',
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_modelo_embedding_fuente UNIQUE (id_modelo, fuente)
);

CREATE INDEX idx_modelo_embedding_modelo ON modelo_embedding (id_modelo);

CREATE INDEX idx_modelo_embedding_hnsw
    ON modelo_embedding USING hnsw (embedding vector_cosine_ops);
