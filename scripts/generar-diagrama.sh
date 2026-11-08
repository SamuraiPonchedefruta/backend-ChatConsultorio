#!/usr/bin/env bash
# Regenera docs/modelo-datos.md a partir del esquema real de la base de datos.
# Requiere el contenedor de Postgres levantado y la app arrancada al menos una vez
# después de la última migración, para que Flyway la haya aplicado.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  source <(tr -d '\r' < .env)
  set +a
fi

CONTENEDOR="${CONTENEDOR:-autobot_postgres}"
USUARIO="${POSTGRES_USER:-postgres}"
BASE="${POSTGRES_DB:-autobot_db}"
SALIDA="docs/modelo-datos.md"
CERCA="$(printf '\140\140\140')"

DIAGRAMA="$(docker exec -i "$CONTENEDOR" psql -U "$USUARIO" -d "$BASE" -At -q < scripts/diagrama_er.sql | tr -d '\r')"
if [ "$(printf '%s\n' "$DIAGRAMA" | head -1)" != "erDiagram" ]; then
  echo "Error: no se pudo generar el diagrama." >&2
  exit 1
fi

VERSION="$(docker exec -i "$CONTENEDOR" psql -U "$USUARIO" -d "$BASE" -At -q \
  -c "SELECT max(version::int) FROM flyway_schema_history WHERE success" | tr -d '\r')"

mkdir -p docs
{
  echo "# Modelo de datos actual"
  echo
  echo "> Generado con \`scripts/generar-diagrama.sh\` desde la base de datos real (última migración aplicada: V${VERSION}). No se edita a mano: se regenera al agregar migraciones."
  echo
  echo "${CERCA}mermaid"
  echo "$DIAGRAMA"
  echo "$CERCA"
} > "$SALIDA"

echo "Diagrama escrito en $SALIDA (migración V${VERSION})"
