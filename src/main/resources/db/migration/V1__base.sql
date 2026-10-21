-- V1__base.sql
-- Migración inicial: extensiones y base común del esquema.

CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()
-- CREATE EXTENSION IF NOT EXISTS vector; -- activar en la épica E2 (pgvector)
