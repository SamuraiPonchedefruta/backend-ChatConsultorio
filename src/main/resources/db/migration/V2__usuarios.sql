-- V2: usuarios, roles y teléfonos (según el diagrama ER).
-- Los correos se guardan en minúsculas y los teléfonos en formato internacional (+507...).

CREATE TABLE rol (
    id_rol     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_rol VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO rol (nombre_rol) VALUES ('CLIENTE'), ('ADMIN');

CREATE TABLE usuario (
    id_usuario     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    correo         VARCHAR(254) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT usuario_correo_minusculas_chk CHECK (correo = lower(correo))
);

CREATE TABLE usuario_rol (
    id_usuario BIGINT  NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    id_rol     INTEGER NOT NULL REFERENCES rol (id_rol),
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE INDEX usuario_rol_id_rol_idx ON usuario_rol (id_rol);

CREATE TABLE telefono_usuario (
    id_telefono  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario   BIGINT      NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    numero       VARCHAR(16) NOT NULL UNIQUE,
    es_principal BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT telefono_numero_formato_chk CHECK (numero ~ '^\+[1-9][0-9]{7,14}$')
);

CREATE INDEX telefono_usuario_id_usuario_idx ON telefono_usuario (id_usuario);

-- Un usuario solo puede tener un teléfono principal.
CREATE UNIQUE INDEX telefono_usuario_un_principal_idx
    ON telefono_usuario (id_usuario) WHERE es_principal;
