#!/usr/bin/env bash
# Respaldo de PostgreSQL (pg_dump formato custom) y subida a S3.
# Uso: ./scripts/backup.sh [--no-upload]
# Variables (de .env o del entorno): POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB,
#   POSTGRES_USER, POSTGRES_PASSWORD, S3_BUCKET, S3_PREFIX (def. backups),
#   BACKUP_DIR (def. ./backups), UPLOADS_DIR (opcional),
#   POSTGRES_CONTAINER (opcional: usa docker exec si no hay pg_dump local).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source <(tr -d '\r' < "$ROOT_DIR/.env")
  set +a
fi

UPLOAD=true
[[ "${1:-}" == "--no-upload" ]] && UPLOAD=false

: "${POSTGRES_DB:?Define POSTGRES_DB}"
: "${POSTGRES_USER:?Define POSTGRES_USER}"
: "${POSTGRES_PASSWORD:?Define POSTGRES_PASSWORD}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
S3_PREFIX="${S3_PREFIX:-backups}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$BACKUP_DIR/${POSTGRES_DB}_${STAMP}.dump"

echo "==> Generando respaldo de '$POSTGRES_DB' en $FILE"
if command -v pg_dump >/dev/null 2>&1; then
  PGPASSWORD="$POSTGRES_PASSWORD" pg_dump \
    -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" -Fc --no-owner -f "$FILE"
elif [[ -n "${POSTGRES_CONTAINER:-}" ]]; then
  docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner > "$FILE"
else
  echo "ERROR: no hay pg_dump local ni POSTGRES_CONTAINER definido." >&2
  exit 1
fi

if [[ ! -s "$FILE" ]]; then
  echo "ERROR: el respaldo quedó vacío." >&2
  rm -f "$FILE"
  exit 1
fi
echo "==> Respaldo generado: $(basename "$FILE") ($(du -h "$FILE" | cut -f1))"

if [[ "$UPLOAD" == false ]]; then
  echo "==> --no-upload: se omite la subida a S3."
  exit 0
fi

: "${S3_BUCKET:?Define S3_BUCKET (o usa --no-upload)}"
command -v aws >/dev/null 2>&1 || { echo "ERROR: falta AWS CLI." >&2; exit 1; }

echo "==> Subiendo a s3://$S3_BUCKET/$S3_PREFIX/"
aws s3 cp "$FILE" "s3://$S3_BUCKET/$S3_PREFIX/$(basename "$FILE")"

if [[ -n "${UPLOADS_DIR:-}" && -d "$UPLOADS_DIR" ]]; then
  echo "==> Sincronizando archivos de $UPLOADS_DIR"
  aws s3 sync "$UPLOADS_DIR" "s3://$S3_BUCKET/uploads/"
fi

echo "==> Listo: s3://$S3_BUCKET/$S3_PREFIX/$(basename "$FILE")"
