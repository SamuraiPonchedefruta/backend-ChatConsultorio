-- V5: lista negra de tokens JWT revocados (logout).
-- Se guarda el identificador (jti) y no el token, y la fecha de expiración permite purgar filas vencidas.
CREATE TABLE token_revocado (
    jti              VARCHAR(36) PRIMARY KEY,
    fecha_revocacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMPTZ NOT NULL
);

CREATE INDEX token_revocado_expiracion_idx ON token_revocado (fecha_expiracion);
