# Respaldo y restauración de la base de datos

Guía para generar, programar y restaurar respaldos de PostgreSQL del backend.

## Resumen

| Qué | Cómo |
|---|---|
| Formato del respaldo | `pg_dump` formato custom (`.dump`) |
| Generar respaldo | `scripts/backup.sh` |
| Restaurar respaldo | `scripts/restore.sh` |
| Respaldo automático de staging | `.github/workflows/backup.yml` (diario, cifrado) |
| Destino de la copia | Artefacto cifrado de GitHub (30 días). Subida a S3 pendiente |

Los dumps contienen datos reales: están ignorados en Git (`backups/` y `*.dump`) y nunca deben subirse al repositorio.

## Generar un respaldo manual

```bash
bash scripts/backup.sh --no-upload
```

Crea `backups/<base>_<fecha UTC>.dump`. El script lee la conexión del archivo `.env` y falla si el archivo queda vacío. Sin `--no-upload` intenta subir la copia a S3 (requiere `S3_BUCKET` y AWS CLI configurado).

## Restaurar un respaldo

> **`restore.sh` REEMPLAZA los objetos de la base de destino** (`--clean --if-exists`). Pruébalo siempre en una base distinta de la de desarrollo, staging o producción.

1. Crear una base de destino vacía (no se crea sola):

   ```bash
   docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE autobot_restore_test;"'
   ```

2. Restaurar el respaldo más reciente sobre ella:

   ```bash
   ARCHIVO=$(ls -t backups/autobot_db_*.dump | head -1)
   RESTORE_DB=autobot_restore_test bash scripts/restore.sh "$ARCHIVO"
   ```

   El script pide escribir el nombre de la base para confirmar. `RESTORE_DB` indica la base destino; si no se define, usa `POSTGRES_DB`.

3. Verificar el resultado (tablas, filas y migraciones).

Host y puerto salen de `POSTGRES_HOST` y `POSTGRES_PORT` del `.env`. Si no hay `pg_restore` local, el script usa el contenedor definido en `POSTGRES_CONTAINER`.

## Prueba de restauración (23/10/2026)

Entorno: PostgreSQL local en Docker (`pgvector/pgvector:pg16`), respaldo `autobot_db_20261023T194004Z.dump` (5555 bytes).

| Comprobación | Base original (`autobot_db`) | Base restaurada (`autobot_restore_test`) |
|---|---|---|
| Tablas en `public` | 2 | 2 |
| Filas en `prueba_respaldo` | 3 | 3 |
| Filas en `flyway_schema_history` | 1 | 1 |

Antes de restaurar, la base de destino estaba limpia (0 tablas; las tablas `prueba_respaldo` y `flyway_schema_history` no existían). Después de ejecutar `restore.sh` no hubo errores y las tres filas de prueba aparecieron con sus datos y marcas de tiempo originales.

**Resultado: la restauración sobre una base limpia se completó con éxito.**

## Respaldo automático de staging

El workflow `.github/workflows/backup.yml` respalda la base de staging (PostgreSQL de Render) todos los días a las 07:00 UTC (02:00 en Panamá), comprueba que el dump es restaurable con `pg_restore --list`, lo cifra con `gpg` y lo guarda como artefacto durante 30 días. El repositorio es público, por eso el cifrado es obligatorio.

Secretos requeridos: `STAGING_DB_HOST`, `STAGING_DB_NAME`, `STAGING_DB_USER`, `STAGING_DB_PASSWORD` y `BACKUP_PASSPHRASE`. Guarda la frase de cifrado en un gestor de contraseñas: sin ella, los respaldos no se pueden recuperar.

Para recuperar un respaldo cifrado (procedimiento aún no probado de punta a punta):

```bash
gh run download <ID-DE-LA-EJECUCION>
printf '%s' "$BACKUP_PASSPHRASE" | gpg --batch --yes --pinentry-mode loopback \
  --passphrase-fd 0 --decrypt -o respaldo.dump archivo.dump.gpg
```

Después se restaura con `restore.sh` como se indica arriba.

## No verificado todavía

- Restauración desde S3 (`s3://...`): requiere crear el bucket (issue #13).
- Restauración de un respaldo descargado del workflow de staging, descifrado con `gpg`.
- Ejecución programada del workflow sin intervención manual (issue #14).

## Problemas frecuentes

| Síntoma | Causa |
|---|---|
| `database "..." does not exist` | La base destino no se creó antes de restaurar |
| `no hay pg_restore local ni POSTGRES_CONTAINER` | Falta `POSTGRES_CONTAINER` en `.env` |
| `server version mismatch` al respaldar | El `pg_dump` es de una versión menor que la del servidor |
| `bash\r: No such file` en Linux | El script se guardó con saltos CRLF; `.gitattributes` fuerza LF para `*.sh` |
