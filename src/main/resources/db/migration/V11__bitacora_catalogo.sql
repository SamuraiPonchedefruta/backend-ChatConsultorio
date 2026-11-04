-- La bitácora deja de ser exclusiva de cuentas: admite acciones sobre modelos del catálogo.
ALTER TABLE bitacora DROP CONSTRAINT ck_bitacora_accion;

ALTER TABLE bitacora ADD CONSTRAINT ck_bitacora_accion
    CHECK (accion IN ('ALTA', 'DESACTIVACION', 'CAMBIO_ROL', 'MODELO_ALTA', 'MODELO_EDICION'));

-- Las acciones sobre modelos no tienen una cuenta afectada.
ALTER TABLE bitacora ALTER COLUMN correo_afectado DROP NOT NULL;

-- Sin llave foránea, igual que los correos: el historial sobrevive aunque se borre el modelo.
ALTER TABLE bitacora ADD COLUMN id_modelo BIGINT;

CREATE INDEX idx_bitacora_id_modelo ON bitacora (id_modelo) WHERE id_modelo IS NOT NULL;
