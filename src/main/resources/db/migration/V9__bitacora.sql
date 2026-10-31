CREATE TABLE bitacora (
    id_bitacora     BIGSERIAL PRIMARY KEY,
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    accion          VARCHAR(30)  NOT NULL,
    correo_admin    VARCHAR(255) NOT NULL,
    correo_afectado VARCHAR(255) NOT NULL,
    detalle         VARCHAR(500),
    CONSTRAINT ck_bitacora_accion
        CHECK (accion IN ('ALTA', 'DESACTIVACION', 'CAMBIO_ROL'))
);

CREATE INDEX idx_bitacora_correo_afectado ON bitacora (correo_afectado);
CREATE INDEX idx_bitacora_fecha ON bitacora (fecha DESC);
