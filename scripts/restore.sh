#!/usr/bin/env bash
# Restaura un respaldo .dump en una base de datos.
# Uso: ./scripts/restore.sh <archivo.dump | s3://bucket/ruta/archivo.dump> [--yes]
# La base destino es RESTORE_DB (por defecto POSTGRES_DB).
# Usa una base distinta de la de producción para probar restauraciones.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source <(tr -d '\r' < "$ROOT_DIR/.env")
  set +a
fi

SOURCE="${1:-}"
[[ -z "$SOURCE" ]] && { echo "Uso: $0 <archivo.dump | s3://...> [--yes]" >&2; exit 1; }
ASSUME_YES=false
[[ "${2:-}" == "--yes" ]] && ASSUME_YES=true

: "${POSTGRES_USER:?Define POSTGRES_USER}"
: "${POSTGRES_PASSWORD:?Define POSTGRES_PASSWORD}"
TARGET_DB="${RESTORE_DB:-${POSTGRES_DB:?Define POSTGRES_DB o RESTORE_DB}}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

FILE="$SOURCE"
if [[ "$SOURCE" == s3://* ]]; then
  command -v aws >/dev/null 2>&1 || { echo "ERROR: falta AWS CLI." >&2; exit 1; }
  FILE="$(mktemp -d)/$(basename "$SOURCE")"
  echo "==> Descargando $SOURCE"
  aws s3 cp "$SOURCE" "$FILE"
fi
[[ -s "$FILE" ]] || { echo "ERROR: no existe o está vacío: $FILE" >&2; exit 1; }

echo "Se restaurará '$(basename "$FILE")' sobre la base '$TARGET_DB' en $POSTGRES_HOST:$POSTGRES_PORT."
echo "Esto REEMPLAZA los objetos existentes de esa base."
if [[ "$ASSUME_YES" == false ]]; then
  read -r -p "Escribe el nombre de la base para confirmar: " CONFIRM
  [[ "$CONFIRM" == "$TARGET_DB" ]] || { echo "Cancelado."; exit 1; }
fi

echo "==> Restaurando..."
if command -v pg_restore >/dev/null 2>&1; then
  PGPASSWORD="$POSTGRES_PASSWORD" pg_restore \
    -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" \
    -d "$TARGET_DB" --clean --if-exists --no-owner "$FILE"
elif [[ -n "${POSTGRES_CONTAINER:-}" ]]; then
  docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
    pg_restore -U "$POSTGRES_USER" -d "$TARGET_DB" --clean --if-exists --no-owner < "$FILE"
else
  echo "ERROR: no hay pg_restore local ni POSTGRES_CONTAINER definido." >&2
  exit 1
fi

echo "==> Restauración completada en '$TARGET_DB'."
