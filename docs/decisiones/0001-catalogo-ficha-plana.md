# 0001. Catálogo de modelos como ficha plana

Estado: aceptada para la implementación, pendiente de confirmar con el docente

## Contexto

El diseño normalizado (diagrama ER a 3FN) propone un catálogo con `MARCA`, `MODELO`, `VEHICULO`
(versiones por año, transmisión y combustible), `CONSUMO_VEHICULO` por tipo de ruta, `VEHICULO_FALLA` y
`MANTENIMIENTO_GENERICO`. La épica E2 se implementó de otra forma:

- `modelo` (marca y nombre como texto) y `ficha_modelo` con consumo, mantenimiento, fallas comunes,
  repuestos y qué revisar como texto libre (V10).
- `rango_precio` por modelo y año, con fuente y fecha (V12).
- `modelo_embedding` con pgvector, un vector por fuente de la ficha (V13).
- Sobre este modelo están la API de administración, la ficha pública con caché, el comparador y el ETL.

## Decisión

Se mantiene el catálogo plano en la implementación. El estado real de la base de datos está en
`docs/modelo-datos.md`, generado con `scripts/generar-diagrama.sh`. El diagrama normalizado sigue siendo
el diseño objetivo del curso y la guía para las épicas que aún no se implementan (citas, productos,
facturación y chat). La diferencia entre ambos queda documentada en este registro.

## Diferencias respecto al diseño normalizado

- Consumo, fallas y mantenimiento son texto: se muestran y se comparan lado a lado, pero no se ordenan ni
  se calculan.
- No hay versiones de un modelo (año, transmisión, combustible). El precio sí es por año.
- La marca es texto dentro de `modelo`, no una tabla aparte.

## Consecuencias

- Los embeddings dependen de `modelo`, así que un cambio de estructura obliga a recalcularlos todos.
- Migrar al diseño normalizado obligaría a rehacer la ficha, la API de administración, el comparador, el
  ETL y los embeddings.

## Cuándo revisarla

- Si el docente exige que la implementación coincida con el diagrama normalizado.
- Si hace falta ordenar o filtrar por consumo, severidad de fallas o costo de reparación, o manejar
  versiones de un mismo modelo. La migración sería incremental (por ejemplo, una tabla `marca` poblada
  desde `modelo.marca`) sin reemplazar lo existente.
