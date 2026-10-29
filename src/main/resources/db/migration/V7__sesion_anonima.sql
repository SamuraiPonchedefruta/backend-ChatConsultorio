-- Migración para trazabilidad y asociación de sesiones anónimas al registrarse
CREATE TABLE IF NOT EXISTS sesion_anonima (
    id_sesion BIGSERIAL PRIMARY KEY,
    identificador_anonimo VARCHAR(255) NOT NULL,
    id_usuario BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    datos_sesion TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asociado_en TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sesion_anonimo_identificador ON sesion_anonima(identificador_anonimo);
