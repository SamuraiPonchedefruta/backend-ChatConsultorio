CREATE TABLE modelo (
    id_modelo      BIGSERIAL    PRIMARY KEY,
    marca          VARCHAR(60)  NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_modelo_marca_nombre UNIQUE (marca, nombre)
);

CREATE TABLE ficha_modelo (
    id_ficha       BIGSERIAL   PRIMARY KEY,
    id_modelo      BIGINT      NOT NULL UNIQUE,
    consumo        TEXT,
    mantenimiento  TEXT,
    fallas_comunes TEXT,
    repuestos      TEXT,
    que_revisar    TEXT,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ficha_modelo
        FOREIGN KEY (id_modelo) REFERENCES modelo (id_modelo) ON DELETE CASCADE
);
