-- V3: textos legales versionados y consentimientos informados.
--
-- Cada fila de consentimiento es una decisión puntual (aceptar o rechazar/revocar).
-- El historial se conserva: cambiar de opinión es insertar una fila nueva, no editar la anterior.
-- Los textos legales publicados tampoco se editan: un cambio de redacción es una versión nueva.

CREATE TABLE texto_legal (
    id_texto_legal INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo           VARCHAR(50)  NOT NULL,
    version        VARCHAR(20)  NOT NULL,
    contenido      TEXT         NOT NULL,
    vigente_desde  TIMESTAMPTZ  NOT NULL,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT texto_legal_tipo_version_uq UNIQUE (tipo, version),
    CONSTRAINT texto_legal_tipo_formato_chk CHECK (tipo ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE TABLE consentimiento (
    id_consentimiento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario        BIGINT      NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    id_texto_legal    INTEGER     NOT NULL REFERENCES texto_legal (id_texto_legal),
    aceptado          BOOLEAN     NOT NULL,
    fecha_decision    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Consulta típica: última decisión de un usuario sobre una versión de texto.
CREATE INDEX consentimiento_usuario_texto_fecha_idx
    ON consentimiento (id_usuario, id_texto_legal, fecha_decision DESC);

CREATE INDEX consentimiento_id_texto_legal_idx ON consentimiento (id_texto_legal);

-- Evidencia inmutable: ni los consentimientos ni los textos publicados se pueden modificar.
CREATE FUNCTION bloquear_modificacion() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION '% es inmutable: registra una fila nueva en lugar de modificar la existente', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER texto_legal_sin_update
    BEFORE UPDATE ON texto_legal
    FOR EACH ROW EXECUTE FUNCTION bloquear_modificacion();

CREATE TRIGGER consentimiento_sin_update
    BEFORE UPDATE ON consentimiento
    FOR EACH ROW EXECUTE FUNCTION bloquear_modificacion();
