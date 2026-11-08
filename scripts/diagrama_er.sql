-- Genera el diagrama ER (Mermaid) a partir del esquema real de la base de datos.
-- Se ejecuta con scripts/generar-diagrama.sh y no depende de ninguna tabla en particular.
WITH tablas AS (
    SELECT c.oid AS rel, c.relname
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind = 'r'
      AND c.relname <> 'flyway_schema_history'
),
claves AS (
    SELECT con.conrelid AS rel, k.attnum,
           CASE con.contype WHEN 'p' THEN 'PK' WHEN 'f' THEN 'FK' WHEN 'u' THEN 'UK' END AS clave
    FROM pg_constraint con
    CROSS JOIN LATERAL unnest(con.conkey) AS k(attnum)
    WHERE con.contype IN ('p', 'f', 'u')
),
columnas AS (
    SELECT t.relname, a.attnum, a.attname,
           trim(both '_' FROM regexp_replace(
               replace(format_type(a.atttypid, a.atttypmod), ' ', '_'), '[(),]+', '_', 'g')) AS tipo,
           (SELECT string_agg(DISTINCT k.clave, ', ' ORDER BY k.clave)
              FROM claves k
             WHERE k.rel = t.rel AND k.attnum = a.attnum) AS claves
    FROM tablas t
    JOIN pg_attribute a ON a.attrelid = t.rel AND a.attnum > 0 AND NOT a.attisdropped
),
relaciones AS (
    SELECT f.conrelid AS hija, f.confrelid AS padre,
           (SELECT string_agg(a.attname, ', ' ORDER BY k.ord)
              FROM unnest(f.conkey) WITH ORDINALITY AS k(attnum, ord)
              JOIN pg_attribute a ON a.attrelid = f.conrelid AND a.attnum = k.attnum) AS columnas,
           (SELECT bool_and(a.attnotnull)
              FROM unnest(f.conkey) AS k(attnum)
              JOIN pg_attribute a ON a.attrelid = f.conrelid AND a.attnum = k.attnum) AS obligatoria,
           EXISTS (SELECT 1 FROM pg_constraint u
                    WHERE u.conrelid = f.conrelid
                      AND u.contype IN ('p', 'u')
                      AND u.conkey = f.conkey) AS unica
    FROM pg_constraint f
    WHERE f.contype = 'f'
)
SELECT linea
FROM (
    SELECT 0 AS orden, 'erDiagram' AS linea

    UNION ALL

    SELECT 1,
           '    ' || p.relname
           || CASE WHEN r.obligatoria THEN ' ||--' ELSE ' |o--' END
           || CASE WHEN r.unica THEN 'o| ' ELSE 'o{ ' END
           || h.relname || ' : "' || r.columnas || '"'
    FROM relaciones r
    JOIN tablas p ON p.rel = r.padre
    JOIN tablas h ON h.rel = r.hija

    UNION ALL

    SELECT 2,
           '    ' || c.relname || ' {' || E'\n'
           || string_agg('        ' || c.tipo || ' ' || c.attname || coalesce(' ' || c.claves, ''),
                         E'\n' ORDER BY c.attnum)
           || E'\n    }'
    FROM columnas c
    GROUP BY c.relname
) x
ORDER BY orden, linea;
