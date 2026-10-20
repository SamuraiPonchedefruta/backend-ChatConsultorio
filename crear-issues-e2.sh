#!/usr/bin/env bash
# Crea los 14 issues de la épica E2 con GitHub CLI.
# Uso:   bash crear-issues-e2.sh          (crea los issues)
#        DRY=1 bash crear-issues-e2.sh    (solo muestra lo que crearía)
set -euo pipefail
 
if [[ "${DRY:-0}" != 1 ]]; then
  gh auth status >/dev/null
  echo "Creando etiquetas..."
  while IFS=: read -r nombre extra color; do
    gh label create "${nombre}:${extra}" --color "$color" --force >/dev/null
  done <<'LABELS'
epica:E2:5319E7
us:E2-01:C5DEF5
us:E2-02:C5DEF5
us:E2-03:C5DEF5
us:E2-04:C5DEF5
us:E2-05:C5DEF5
tipo:backend:1D76DB
tipo:frontend:0E8A16
tipo:etl:FBCA04
tipo:ci:6F42C1
tipo:db:D93F0B
prioridad:alta:B60205
prioridad:media:FEF2C0
LABELS
  gh label create "bloqueado" --color 000000 --force >/dev/null
fi
 
# Convierte "a|b|c" en una lista Markdown con el prefijo dado
lista() {
  local IFS='|' item
  for item in $1; do printf -- '- %s%s\n' "$2" "$item"; done
}
 
# crear ID TITULO TIPO PRIORIDAD HISTORIA QUE CRITERIOS ARCHIVOS DEPENDE BLOQUEA EST RAMA
crear() {
  local id="$1" titulo="$2" tipo="$3" prio="$4" hist="$5" que="$6"
  local crit="$7" arch="$8" dep="$9" blq="${10}" est="${11}" rama="${12}"
  local epica="${id%%-*}"
  local labels="epica:${epica},tipo:${tipo},prioridad:${prio}"
  if [[ "$hist" == US-* ]]; then labels+=",us:${hist#US-}"; fi
 
  local body
  body="$(cat <<EOF
## Historia de usuario
${hist}
 
## Qué hay que hacer
${que}
 
## Hecho cuando
$(lista "$crit" '[ ] ')
- [ ] Las pruebas pasan en CI
 
## Archivos a tocar (máx. 5)
$(lista "$arch" '')
 
## Dependencias
- **Depende de:** ${dep}
- **Bloquea:** ${blq}
 
## Detalles
| Épica | Tipo | Prioridad | Estimación | Rama sugerida |
|---|---|---|---|---|
| ${epica} | ${tipo} | ${prio} | ${est} h | \`${rama}\` |
EOF
)"
 
  if [[ "${DRY:-0}" == 1 ]]; then
    printf '=== [%s] %s\n    labels: %s\n%s\n\n' "$id" "$titulo" "$labels" "$body"
  else
    gh issue create --title "[${id}] ${titulo}" --label "$labels" \
      --assignee "@me" --body "$body"
    sleep 1
  fi
}
 
# ---------- US-E2-01 · Gestión del catálogo de modelos ----------
crear E2-01-A 'Crear modelo de datos del catálogo' db alta US-E2-01 \
  'Crear la migración, las entidades y el repositorio del catálogo con los campos: modelo, consumo, mantenimiento, fallas comunes, repuestos, qué revisar y `actualizado_en`.' \
  'Flyway aplica `V10` sin errores|Se guarda un modelo con `actualizado_en` automático' \
  '`V10__catalogo.sql`|`Modelo.java`|`FichaModelo.java`|`ModeloRepository.java`' \
  'E0-03-A' 'E2-01-B, E2-02-A, E2-03-A, E2-05-A, E2-05-B' 3 'feat/E2-01-A-modelo-catalogo'
 
crear E2-01-B 'Crear API de administración de modelos' backend alta US-E2-01 \
  'Crear y editar modelos del catálogo mediante endpoints restringidos al rol administrador.' \
  'Crear o editar responde 200|Un usuario no administrador recibe 403|El cambio se ve sin redespliegue' \
  '`AdminModeloController.java`|`ModeloService.java`|`ModeloRequest.java`|`AdminModeloTest.java`' \
  'E2-01-A, E1-06-A' 'E2-01-C, E2-03-C' 4 'feat/E2-01-B-api-admin-modelos'
 
crear E2-01-C 'Registrar cambios del catálogo en la bitácora' backend media US-E2-01 \
  'Registrar en la bitácora cada alta o edición de un modelo, con el usuario y la fecha.' \
  'Cada alta o edición queda registrada con usuario y fecha' \
  '`ModeloService.java`|`ModeloBitacoraTest.java`' \
  'E2-01-B, E1-07-B' 'Ninguno' 2 'feat/E2-01-C-bitacora-catalogo'
 
# ---------- US-E2-02 · Rangos de precio de referencia ----------
crear E2-02-A 'Crear tabla y entidad de rango de precio' db media US-E2-02 \
  'Crear la tabla y la entidad de rango de precio por modelo y año, con fuente y fecha, y la restricción `min <= max`.' \
  'La base de datos rechaza un mínimo mayor que el máximo|Cada rango guarda su fuente y su fecha' \
  '`V11__rango_precio.sql`|`RangoPrecio.java`|`RangoPrecioRepository.java`' \
  'E2-01-A' 'E2-02-B' 3 'feat/E2-02-A-rango-precio-tabla'
 
crear E2-02-B 'Crear endpoint para guardar rangos de precio' backend media US-E2-02 \
  'Crear el endpoint que guarda el rango de precio y valida que el modelo y el año existan.' \
  'Guarda el rango con fuente y fecha|Rechaza un rango inválido con un mensaje claro|Un usuario no administrador recibe 403' \
  '`RangoPrecioController.java`|`RangoPrecioService.java`|`RangoPrecioTest.java`' \
  'E2-02-A, E1-06-A' 'Ninguno' 3 'feat/E2-02-B-endpoint-rango-precio'
 
# ---------- US-E2-03 · Ficha de modelo ----------
crear E2-03-A 'Crear endpoint público de ficha de modelo' backend alta US-E2-03 \
  'Crear `GET /modelos/{id}` público que devuelve todos los campos de la ficha.' \
  'Responde la ficha completa con la fecha de actualización sin necesidad de token' \
  '`ModeloPublicController.java`|`FichaDto.java`|`ModeloPublicTest.java`|`SecurityConfig.java`' \
  'E2-01-A, E1-05-A' 'E2-03-B, E2-03-C, E2-04-A' 3 'feat/E2-03-A-ficha-publica'
 
crear E2-03-B 'Crear pantalla de ficha optimizada para móvil' frontend media US-E2-03 \
  'Crear la pantalla de ficha de modelo, optimizada para móvil.' \
  'Muestra todos los campos de la ficha|Carga en menos de 3 s en 3G simulado (RNF1)' \
  '`FichaModelo.vue/jsx`|`fichaApi.js`|`FichaModelo.css`' \
  'E2-03-A' 'E2-04-B' 4 'feat/E2-03-B-pantalla-ficha'
 
crear E2-03-C 'Agregar caché y compresión a la ficha' backend media US-E2-03 \
  'Agregar caché y compresión a la respuesta de la ficha de modelo.' \
  'La segunda consulta sale de caché|Una edición del administrador invalida la caché' \
  '`CacheConfig.java`|`application.yml`|`FichaCacheTest.java`' \
  'E2-03-A, E2-01-B' 'Ninguno' 3 'feat/E2-03-C-cache-ficha'
 
# ---------- US-E2-04 · Comparador de modelos ----------
crear E2-04-A 'Crear endpoint comparador de modelos' backend alta US-E2-04 \
  'Crear `GET /modelos/comparar?ids=` que acepte un mínimo de 2 modelos y un máximo configurable.' \
  'Devuelve los modelos alineados para comparar|Responde 400 fuera del rango permitido' \
  '`ComparadorController.java`|`ComparadorService.java`|`application.yml`|`ComparadorTest.java`' \
  'E2-03-A' 'E2-04-B' 3 'feat/E2-04-A-comparador-api'
 
crear E2-04-B 'Crear pantalla de comparación lado a lado' frontend alta US-E2-04 \
  'Crear la pantalla de comparación lado a lado, con desplazamiento horizontal en móvil.' \
  'En 360 px de ancho se recorre sin romper el diseño' \
  '`Comparador.vue/jsx`|`comparadorApi.js`|`Comparador.css`' \
  'E2-04-A, E2-03-B' 'Ninguno' 4 'feat/E2-04-B-pantalla-comparador'
 
# ---------- US-E2-05 · Carga y vectorización del catálogo (ETL) ----------
crear E2-05-A 'Crear tabla de embeddings con pgvector' db media US-E2-05 \
  'Crear la tabla de embeddings con pgvector y su índice. Definir aquí el modelo de embeddings y la dimensión del vector, porque la dimensión va en la columna.' \
  'Se guarda un vector|La búsqueda por similitud devuelve resultados|Queda documentado el modelo y la dimensión elegidos' \
  '`V12__embeddings.sql`|`ModeloEmbedding.java`|`EmbeddingRepository.java`' \
  'E2-01-A, E0-01-A' 'E2-05-C' 3 'feat/E2-05-A-tabla-embeddings'
 
crear E2-05-B 'Limpiar y normalizar archivos fuente del ETL' etl media US-E2-05 \
  'Limpiar y normalizar los archivos fuente del catálogo, con un log de registros inválidos.' \
  'Los registros inválidos van al log|Los registros válidos salen normalizados|El log no se sube al repositorio' \
  '`etl/limpiar.py`|`etl/esquema.py`|`etl/test_limpiar.py`|`.gitignore` (agregar `data/invalidos.log`)' \
  'E2-01-A' 'E2-05-C' 4 'feat/E2-05-B-limpieza-etl'
 
crear E2-05-C 'Vectorizar e ingresar el catálogo de forma idempotente' etl media US-E2-05 \
  'Vectorizar los modelos limpios e ingresarlos a PostgreSQL de forma idempotente.' \
  'Correrlo dos veces no duplica registros|Cada modelo tiene su embedding' \
  '`etl/cargar.py`|`etl/embeddings.py`|`etl/test_idempotencia.py`' \
  'E2-05-A, E2-05-B' 'E2-CI-A' 4 'feat/E2-05-C-carga-vectorizacion'
 
# ---------- CI para E2 ----------
crear E2-CI-A 'Agregar pruebas del ETL al pipeline' ci media 'N/A (CI de la épica E2)' \
  'Agregar al pipeline de GitHub Actions las pruebas en Python del ETL, con Postgres + pgvector como servicio.' \
  'Las pruebas del ETL corren en cada PR|Bloquean el merge si fallan' \
  '`.github/workflows/ci.yml`|`etl/requirements.txt`' \
  'E2-05-C, E0-04-A' 'Ninguno' 3 'ci/E2-CI-A-etl-en-pipeline'
 
echo "Listo."
 
