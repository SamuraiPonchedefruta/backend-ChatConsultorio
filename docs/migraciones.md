# Convención de migraciones (Flyway)

## Ubicación
`src/main/resources/db/migration/`

## Nombre de archivo
`V<versión>__<descripcion_en_snake_case>.sql`

Ejemplos:
- `V1__base.sql`
- `V2__crear_tabla_modelo.sql`
- `V3__agregar_indice_modelo_marca.sql`

Reglas:
- Prefijo `V` en mayúscula, versión entera consecutiva, **doble guion bajo** (`__`).
- Descripción corta, en minúsculas y con guiones bajos.
- Una migración = un cambio lógico.

## Reglas de oro
1. **Nunca se edita una migración ya aplicada o mergeada**: Flyway valida el checksum y la app no arranca. Los cambios van en una nueva versión.
2. Si dos ramas crean la misma versión, quien mergea después renumera la suya.
3. Las migraciones deben ser idempotentes cuando sea razonable (`IF NOT EXISTS`).
4. Los datos de prueba no van en `db/migration`.

## Verificación
```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```
