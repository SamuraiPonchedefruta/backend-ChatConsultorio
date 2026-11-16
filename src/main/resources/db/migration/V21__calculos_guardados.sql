CREATE TABLE calculos_guardados (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    modelo_id   BIGINT      NOT NULL REFERENCES modelo(id_modelo),
    supuestos   JSONB       NOT NULL,
    resultado   JSONB       NOT NULL,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_calculos_guardados_usuario
    ON calculos_guardados (usuario_id, creado_en DESC);
