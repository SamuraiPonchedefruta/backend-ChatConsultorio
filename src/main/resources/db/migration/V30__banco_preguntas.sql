-- E4-10-A: modelo versionado del banco de preguntas
-- Zona -> Pregunta -> Versiones de pregunta
-- Convencion: id_<entidad> como PK (igual que el diagrama ER del proyecto)

CREATE TABLE IF NOT EXISTS zona (
    id_zona  SERIAL       PRIMARY KEY,
    codigo   VARCHAR(30)  NOT NULL UNIQUE,
    nombre   VARCHAR(80)  NOT NULL,
    orden    SMALLINT     NOT NULL
);

CREATE TABLE IF NOT EXISTS pregunta (
    id_pregunta  BIGSERIAL    PRIMARY KEY,
    id_zona      INT          NOT NULL REFERENCES zona (id_zona),
    codigo       VARCHAR(40)  NOT NULL UNIQUE,
    activa       BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pregunta_zona ON pregunta (id_zona);

CREATE TABLE IF NOT EXISTS pregunta_version (
    id_pregunta_version  BIGSERIAL       PRIMARY KEY,
    id_pregunta          BIGINT          NOT NULL REFERENCES pregunta (id_pregunta) ON DELETE CASCADE,
    numero               INTEGER         NOT NULL CHECK (numero >= 1),
    texto                TEXT            NOT NULL,
    imagen_url           VARCHAR(500),
    gravedad             SMALLINT        NOT NULL CHECK (gravedad BETWEEN 1 AND 5),
    peso                 NUMERIC(5, 2)   NOT NULL CHECK (peso > 0),
    explicacion          TEXT,
    bandera_roja         BOOLEAN         NOT NULL DEFAULT FALSE,
    costo_reparacion     NUMERIC(12, 2)  CHECK (costo_reparacion >= 0),
    vigente              BOOLEAN         NOT NULL DEFAULT FALSE,
    creado_en            TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT uq_pregunta_version_numero UNIQUE (id_pregunta, numero)
);

-- Solo una version vigente por pregunta
CREATE UNIQUE INDEX IF NOT EXISTS uq_pregunta_version_vigente
    ON pregunta_version (id_pregunta) WHERE vigente;

-- Las 6 zonas del banco
INSERT INTO zona (codigo, nombre, orden) VALUES
    ('MOTOR',        'Motor',        1),
    ('TRANSMISION',  'Transmisión',  2),
    ('FRENOS',       'Frenos',       3),
    ('SUSPENSION',   'Suspensión',   4),
    ('ELECTRICO',    'Eléctrico',    5),
    ('CARROCERIA',   'Carrocería',   6)
ON CONFLICT (codigo) DO NOTHING;
