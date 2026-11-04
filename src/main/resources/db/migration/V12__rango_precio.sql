CREATE TABLE rango_precio (
    id_rango     BIGSERIAL     PRIMARY KEY,
    id_modelo    BIGINT        NOT NULL,
    anio         INTEGER       NOT NULL,
    precio_min   NUMERIC(12,2) NOT NULL,
    precio_max   NUMERIC(12,2) NOT NULL,
    fuente       VARCHAR(200)  NOT NULL,
    fecha_fuente DATE          NOT NULL,
    CONSTRAINT fk_rango_precio_modelo
        FOREIGN KEY (id_modelo) REFERENCES modelo (id_modelo) ON DELETE CASCADE,
    CONSTRAINT ck_rango_precio_min_max CHECK (precio_min <= precio_max),
    CONSTRAINT ck_rango_precio_min_positivo CHECK (precio_min >= 0),
    CONSTRAINT ck_rango_precio_anio CHECK (anio BETWEEN 1900 AND 2100)
);

CREATE INDEX idx_rango_precio_modelo_anio ON rango_precio (id_modelo, anio);

COMMENT ON COLUMN rango_precio.precio_min IS 'Precio mínimo en USD';
COMMENT ON COLUMN rango_precio.precio_max IS 'Precio máximo en USD';
COMMENT ON COLUMN rango_precio.fecha_fuente IS 'Fecha en que la fuente publicó o registró el precio';
