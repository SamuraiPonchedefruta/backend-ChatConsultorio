#!/usr/bin/env bash
# Crea los 30 issues de las épicas E3 (8), E4 (20) y CI (2) con GitHub CLI.
# Uso:   bash crear-issues-e3-e4.sh          (crea los issues)
#        DRY=1 bash crear-issues-e3-e4.sh    (solo muestra lo que crearía)
set -euo pipefail

if [[ "${DRY:-0}" != 1 ]]; then
  gh auth status >/dev/null
  echo "Creando etiquetas..."
  while IFS=: read -r nombre extra color; do
    gh label create "${nombre}:${extra}" --color "$color" --force >/dev/null
  done <<'LABELS'
epica:E3:5319E7
epica:E4:5319E7
us:E3-01:C5DEF5
us:E3-02:C5DEF5
us:E3-03:C5DEF5
us:E4-01:C5DEF5
us:E4-02:C5DEF5
us:E4-03:C5DEF5
us:E4-04:C5DEF5
us:E4-05:C5DEF5
us:E4-06:C5DEF5
us:E4-07:C5DEF5
us:E4-08:C5DEF5
us:E4-09:C5DEF5
us:E4-10:C5DEF5
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
    sleep 2
  fi
}

# =====================================================================
# E3 · Calculadora de costo total de propiedad
# =====================================================================

# ---------- US-E3-01 · Cálculo del TCO ----------
crear E3-01-A 'Crear tabla de supuestos por defecto del TCO' db alta US-E3-01 \
  'Crear la tabla de supuestos por defecto (km por año, precio de combustible, seguro y gastos anuales) con la fecha de los datos, editable sin tocar código.' \
  'Flyway aplica `V20` sin errores|Los supuestos se editan sin tocar código|Cada supuesto guarda la fecha de sus datos' \
  '`V20__supuestos_tco.sql`|`SupuestoTco.java`|`SupuestoTcoRepository.java`' \
  'E0-03-A' 'E3-01-B' 2 'feat/E3-01-A-supuestos-tco'

crear E3-01-B 'Crear servicio de cálculo del costo total de propiedad' backend alta US-E3-01 \
  'Crear el servicio que calcula el TCO con cinco rubros: compra, combustible, mantenimiento, seguro y gastos anuales. El precio de compra se toma del rango de referencia de E2-02; si prefieren que el usuario lo escriba, se elimina esa dependencia.' \
  'Devuelve el desglose de los cinco rubros|Las pruebas cubren casos normales y casos límite de la fórmula' \
  '`CostoTotalService.java`|`CostoTotalResultado.java`|`CostoTotalServiceTest.java`' \
  'E3-01-A, E2-01-A, E2-02-A' 'E3-01-C, E4-CI-A' 4 'feat/E3-01-B-servicio-tco'

crear E3-01-C 'Crear endpoint público de la calculadora de TCO' backend alta US-E3-01 \
  'Crear `POST /calculadora/tco` público que devuelve el desglose junto con el alcance, las limitaciones y la fecha de los datos.' \
  'Responde sin token con el desglose|La respuesta incluye los tres textos informativos: alcance, limitaciones y fecha de datos' \
  '`CalculadoraController.java`|`CalculoRequest.java`|`SecurityConfig.java`|`CalculadoraApiTest.java`' \
  'E3-01-B, E1-05-A' 'E3-01-D, E3-02-A, E3-03-A' 3 'feat/E3-01-C-endpoint-calculadora'

crear E3-01-D 'Crear pantalla de resultado del TCO' frontend alta US-E3-01 \
  'Crear la pantalla de resultado con el desglose, el alcance y las limitaciones.' \
  'Al elegir un modelo se muestran los cinco rubros|Se muestra la fecha de los datos' \
  '`Calculadora.vue`|`DesgloseTco.vue`|`calculadoraApi.js`|`Calculadora.css`' \
  'E3-01-C' 'E3-02-B' 4 'feat/E3-01-D-pantalla-tco'

# ---------- US-E3-02 · Supuestos del usuario ----------
crear E3-02-A 'Validar y aceptar supuestos del usuario en la calculadora' backend media US-E3-02 \
  'Validar y aceptar los supuestos del usuario (km por año, combustible y seguro) devolviendo errores claros.' \
  'Un valor inválido devuelve 400 con un mensaje claro|Un valor válido recalcula el resultado' \
  '`CalculoRequest.java`|`CalculadoraController.java`|`CalculoValidacionTest.java`' \
  'E3-01-C, E0-02-C' 'E3-02-B' 3 'feat/E3-02-A-validar-supuestos'

crear E3-02-B 'Crear controles de supuestos con recálculo en pantalla' frontend media US-E3-02 \
  'Agregar los controles de supuestos en pantalla, con recálculo automático y el error visible junto al campo.' \
  'Al cambiar un valor el resultado se actualiza|El error se ve junto al campo' \
  '`SupuestosForm.vue`|`Calculadora.vue`|`calculadoraApi.js`' \
  'E3-02-A, E3-01-D' 'Ninguno' 3 'feat/E3-02-B-controles-supuestos'

# ---------- US-E3-03 · Guardar cálculo ----------
crear E3-03-A 'Crear guardado de cálculos en la cuenta del cliente' backend media US-E3-03 \
  'Crear la tabla, la entidad y los endpoints para guardar y consultar los cálculos de cada cliente.' \
  'Un cliente guarda un cálculo|Un cliente consulta solo sus propios cálculos' \
  '`V21__calculos_guardados.sql`|`CalculoGuardado.java`|`CalculoGuardadoRepository.java`|`CalculoGuardadoController.java`|`CalculoGuardadoTest.java`' \
  'E3-01-C, E1-03-A' 'E3-03-B' 4 'feat/E3-03-A-calculos-guardados'

crear E3-03-B 'Crear botón de guardar con redirección al registro' frontend media US-E3-03 \
  'Agregar el botón de guardar cálculo y redirigir al visitante al registro sin perder el cálculo. Requiere la sesión anónima de E1-05-C.' \
  'Un visitante se registra y su cálculo aparece guardado' \
  '`GuardarCalculo.vue`|`calculadoraApi.js`|`GuardarCalculo.test.js`' \
  'E3-03-A, E1-05-C' 'Ninguno' 3 'feat/E3-03-B-guardar-calculo'

# =====================================================================
# E4 · Semáforo del comprador
# =====================================================================

# ---------- US-E4-10 · Banco de preguntas ----------
crear E4-10-A 'Crear modelo versionado del banco de preguntas' db alta US-E4-10 \
  'Crear el modelo versionado del banco: zonas, preguntas y versiones de pregunta (texto, imagen, gravedad, peso, explicación, bandera roja y costo de reparación).' \
  'Se guarda una pregunta con varias versiones|Las 6 zonas existen tras aplicar la migración' \
  '`V30__banco_preguntas.sql`|`Zona.java`|`Pregunta.java`|`PreguntaVersion.java`|`PreguntaRepository.java`' \
  'E0-03-A' 'E4-10-B, E4-10-C, E4-01-A, E4-02-A, E4-03-A' 4 'feat/E4-10-A-banco-preguntas'

crear E4-10-B 'Crear API de administración del banco de preguntas' backend media US-E4-10 \
  'Crear la API de administración donde editar una pregunta crea una versión nueva y queda registrada en la bitácora.' \
  'Editar crea una versión nueva|Las versiones antiguas no cambian|Se registra el usuario y la fecha del cambio' \
  '`AdminPreguntaController.java`|`PreguntaService.java`|`PreguntaRequest.java`|`AdminPreguntaTest.java`' \
  'E4-10-A, E1-06-A, E1-07-B' 'Ninguno' 4 'feat/E4-10-B-api-admin-preguntas'

crear E4-10-C 'Cargar el banco inicial de preguntas por zona' db media US-E4-10 \
  'Cargar el banco inicial de preguntas para cada una de las 6 zonas.' \
  'Las 6 zonas tienen preguntas|Cada pregunta tiene todos sus campos completos' \
  '`V31__seed_preguntas.sql`' \
  'E4-10-A' 'E4-02-C' 3 'feat/E4-10-C-seed-preguntas'

# ---------- US-E4-01 · Iniciar autoevaluación ----------
crear E4-01-A 'Crear autoevaluación con tabla y endpoint de inicio' backend media US-E4-01 \
  'Crear la tabla y el endpoint de la autoevaluación (modelo del catálogo, año, kilometraje y precio), con un mensaje que indique el dato faltante.' \
  'Si falta un dato responde 400 indicando cuál|Con todos los datos crea la autoevaluación' \
  '`V32__autoevaluaciones.sql`|`Autoevaluacion.java`|`AutoevaluacionRepository.java`|`AutoevaluacionController.java`|`IniciarAutoevaluacionTest.java`' \
  'E2-01-A, E4-10-A, E1-05-A' 'E4-01-B, E4-02-A, E4-07-A' 4 'feat/E4-01-A-iniciar-autoevaluacion'

crear E4-01-B 'Crear pantalla del formulario inicial de autoevaluación' frontend media US-E4-01 \
  'Crear la pantalla del formulario inicial de la autoevaluación.' \
  'No avanza si falta un dato|El mensaje nombra el dato que falta' \
  '`AutoevaluacionInicio.vue`|`autoevaluacionApi.js`|`AutoevaluacionInicio.css`' \
  'E4-01-A' 'Ninguno' 3 'feat/E4-01-B-pantalla-inicio'

# ---------- US-E4-02 · Responder el cuestionario ----------
crear E4-02-A 'Crear tabla de respuestas del cuestionario' db media US-E4-02 \
  'Crear la tabla de respuestas (Sí, No, Un poco, No sé) guardando la versión de la pregunta respondida.' \
  'Cada respuesta queda ligada a su versión de pregunta|Las cuatro opciones de respuesta se guardan correctamente' \
  '`V33__respuestas.sql`|`Respuesta.java`|`RespuestaRepository.java`' \
  'E4-01-A, E4-10-A' 'E4-02-B' 2 'feat/E4-02-A-tabla-respuestas'

crear E4-02-B 'Crear endpoints para responder, retroceder y ver progreso' backend media US-E4-02 \
  'Crear los endpoints para responder una pregunta, retroceder y consultar el progreso.' \
  'Retroceder conserva las respuestas|El progreso devuelto es correcto' \
  '`RespuestaController.java`|`RespuestaService.java`|`ProgresoDto.java`|`RespuestaApiTest.java`' \
  'E4-02-A' 'E4-02-C, E4-03-B' 4 'feat/E4-02-B-endpoints-respuestas'

crear E4-02-C 'Crear pantalla del cuestionario, una pregunta por pantalla' frontend media US-E4-02 \
  'Crear la pantalla del cuestionario: una pregunta por pantalla con imagen, ayuda, progreso y retroceso.' \
  'Se completa el semáforo en móvil sin ayuda externa|Es operable con teclado y lector de pantalla' \
  '`PreguntaPantalla.vue`|`ProgresoZonas.vue`|`cuestionarioApi.js`|`Cuestionario.css`' \
  'E4-02-B, E4-10-C' 'E4-CI-B' 5 'feat/E4-02-C-pantalla-cuestionario'

# ---------- US-E4-03, E4-04, E4-05 y E4-07 · Cálculo del resultado ----------
crear E4-03-A 'Crear motor de cálculo del semáforo global y por zona' backend alta US-E4-03 \
  'Crear el motor de cálculo global y por zona (favorable, gravedad y peso). Antes de empezar hay que fijar a partir de qué puntaje es verde, amarillo o rojo y cómo se pondera la respuesta «Un poco»; sin eso no se pueden escribir las pruebas de casos límite.' \
  'Todo favorable da verde|Todo desfavorable da rojo|Los bordes de cada umbral están probados' \
  '`SemaforoCalculator.java`|`ResultadoSemaforo.java`|`ZonaResultado.java`|`SemaforoCalculatorTest.java`' \
  'E4-10-A' 'E4-03-B, E4-04-A, E4-05-A, E4-CI-A' 5 'feat/E4-03-A-motor-semaforo'

crear E4-03-B 'Guardar el resultado y verificar su coherencia' backend alta US-E4-03 \
  'Guardar el resultado del semáforo y verificar que coincide con las respuestas que lo originan.' \
  'Recalcular desde las respuestas guardadas da el mismo resultado|El resultado queda guardado con su autoevaluación' \
  '`V34__resultado_semaforo.sql`|`ResultadoEntity.java`|`ResultadoRepository.java`|`SemaforoService.java`|`SemaforoCoherenciaTest.java`' \
  'E4-03-A, E4-02-B' 'E4-06-A, E4-08-A, E4-09-A' 4 'feat/E4-03-B-guardar-resultado'

crear E4-04-A 'Agregar bandera roja crítica al semáforo' backend media US-E4-04 \
  'Hacer que una bandera roja crítica ponga el resultado global en rojo y destaque el hallazgo.' \
  'Con puntaje alto y una bandera crítica el resultado global es rojo|Se identifica el hallazgo crítico' \
  '`SemaforoCalculator.java`|`HallazgoCritico.java`|`BanderaRojaTest.java`' \
  'E4-03-A' 'E4-06-A' 3 'feat/E4-04-A-bandera-roja'

crear E4-05-A 'Calcular el nivel de confianza según los No sé' backend media US-E4-05 \
  'Calcular el nivel de confianza del resultado según la cantidad de respuestas «No sé», con una etiqueta de texto.' \
  'El nivel baja al subir los «No sé»|Devuelve una etiqueta de texto' \
  '`ConfianzaCalculator.java`|`NivelConfianza.java`|`ConfianzaCalculatorTest.java`' \
  'E4-03-A' 'E4-06-A' 3 'feat/E4-05-A-nivel-confianza'

crear E4-07-A 'Comparar el precio pedido con el rango de referencia' backend media US-E4-07 \
  'Comparar el precio pedido con el rango de referencia y devolver uno de cuatro estados (dentro, por encima, por debajo o sin rango), con la fecha del rango.' \
  'Los cuatro estados están probados|Se muestra la fecha del rango' \
  '`ComparadorPrecioService.java`|`EstadoPrecio.java`|`ComparadorPrecioTest.java`' \
  'E2-02-A, E4-01-A' 'E4-06-A' 3 'feat/E4-07-A-comparar-precio'

# ---------- US-E4-06, E4-08 y E4-09 · Resultado, compartir e historial ----------
crear E4-06-A 'Crear endpoint del resultado completo de la autoevaluación' backend alta US-E4-06 \
  'Crear `GET /autoevaluaciones/{id}/resultado` con color, recomendación, hallazgos, costos de reparación, confianza, estado del precio y aviso orientativo.' \
  'La respuesta trae todos los campos|Siempre incluye el aviso orientativo' \
  '`ResultadoController.java`|`ResultadoDto.java`|`ResultadoService.java`|`ResultadoApiTest.java`' \
  'E4-03-B, E4-04-A, E4-05-A, E4-07-A' 'E4-06-B, E4-08-B' 4 'feat/E4-06-A-endpoint-resultado'

crear E4-06-B 'Crear pantalla de resultado del semáforo' frontend alta US-E4-06 \
  'Crear la pantalla de resultado: color con texto e ícono, hallazgos, costos y aviso visible.' \
  'Se entiende sin depender de los colores|El aviso orientativo aparece siempre' \
  '`ResultadoSemaforo.vue`|`HallazgoItem.vue`|`resultadoApi.js`|`Resultado.css`' \
  'E4-06-A' 'E4-08-C, E4-CI-B' 5 'feat/E4-06-B-pantalla-resultado'

crear E4-08-A 'Crear tabla de enlaces compartidos del resultado' db media US-E4-08 \
  'Crear la tabla de enlaces compartidos con un token sin datos personales, fecha de expiración y revocación.' \
  'El token no contiene datos personales|Guarda su fecha de vencimiento' \
  '`V35__enlaces_compartidos.sql`|`EnlaceCompartido.java`|`EnlaceCompartidoRepository.java`' \
  'E4-03-B' 'E4-08-B' 3 'feat/E4-08-A-tabla-enlaces'

crear E4-08-B 'Crear endpoints para compartir, ver y revocar el enlace' backend media US-E4-08 \
  'Crear los endpoints para crear el enlace, verlo públicamente y revocarlo.' \
  'El enlace abre sin mostrar datos personales|El enlace expira en su fecha|El enlace deja de funcionar al revocarlo' \
  '`CompartirController.java`|`CompartirService.java`|`SecurityConfig.java`|`CompartirTest.java`' \
  'E4-08-A, E4-06-A' 'E4-08-C' 4 'feat/E4-08-B-endpoints-compartir'

crear E4-08-C 'Crear botón para compartir por WhatsApp o enlace' frontend alta US-E4-08 \
  'Agregar el botón para compartir el resultado por WhatsApp o copiando el enlace.' \
  'Genera el mensaje de WhatsApp|Copia el enlace al portapapeles' \
  '`CompartirResultado.vue`|`compartirApi.js`|`CompartirResultado.test.js`' \
  'E4-08-B, E4-06-B' 'Ninguno' 3 'feat/E4-08-C-boton-compartir'

crear E4-09-A 'Crear endpoint del historial de autoevaluaciones' backend media US-E4-09 \
  'Crear `GET /autoevaluaciones` con el historial, devolviendo solo los registros del usuario autenticado.' \
  'Un cliente nunca ve registros de otro cliente|Devuelve el historial propio con su resultado' \
  '`HistorialController.java`|`HistorialService.java`|`HistorialTest.java`' \
  'E4-03-B, E1-03-A' 'E4-09-B' 3 'feat/E4-09-A-endpoint-historial'

crear E4-09-B 'Crear pantalla de historial' frontend media US-E4-09 \
  'Crear la pantalla que lista las autoevaluaciones previas.' \
  'Lista las autoevaluaciones previas con su color y su texto' \
  '`Historial.vue`|`historialApi.js`' \
  'E4-09-A' 'Ninguno' 3 'feat/E4-09-B-pantalla-historial'

# =====================================================================
# CI para E3 y E4
# =====================================================================
crear E4-CI-A 'Agregar umbral de cobertura JaCoCo al pipeline' ci media 'N/A (CI de las épicas E3 y E4)' \
  'Agregar un umbral de cobertura con JaCoCo para los módulos `calculadora` y `semaforo` en el pipeline.' \
  'El PR falla si la cobertura de esos módulos baja del umbral definido|El umbral queda documentado en el `pom.xml`' \
  '`.github/workflows/ci.yml`|`pom.xml`' \
  'E3-01-B, E4-03-A, E0-04-A' 'Ninguno' 2 'ci/E4-CI-A-cobertura-jacoco'

crear E4-CI-B 'Agregar revisión automática de accesibilidad al pipeline' ci media 'N/A (CI de las épicas E3 y E4)' \
  'Agregar una revisión automática de accesibilidad (axe o Lighthouse) sobre el cuestionario y el resultado.' \
  'El check reporta fallos de contraste|El check reporta fallos de etiquetas|El check reporta fallos de navegación por teclado' \
  '`.github/workflows/ci.yml`|`lighthouserc.json`' \
  'E4-02-C, E4-06-B' 'Ninguno' 3 'ci/E4-CI-B-accesibilidad'

echo "Listo."