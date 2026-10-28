-- V6: tokens de restablecimiento de contraseña de un solo uso.
--
-- El token en claro nunca se guarda: solo su hash SHA-256 en hexadecimal. Si alguien lee esta tabla,
-- no obtiene ningún token utilizable.
-- usado_en se llena cuando el token se consume o cuando se invalida porque el usuario pidió uno nuevo.
-- Un token es válido solo si usado_en es NULL y expira_en está en el futuro.

CREATE TABLE reset_token (
    id_reset_token BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario     BIGINT      NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    token_hash     VARCHAR(64) NOT NULL UNIQUE,
    expira_en      TIMESTAMPTZ NOT NULL,
    usado_en       TIMESTAMPTZ,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reset_token_hash_formato_chk CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT reset_token_vigencia_chk CHECK (expira_en > creado_en)
);

CREATE INDEX reset_token_id_usuario_idx ON reset_token (id_usuario);
