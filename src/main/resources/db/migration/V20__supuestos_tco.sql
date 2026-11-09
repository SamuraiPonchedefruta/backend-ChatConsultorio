CREATE TABLE supuesto_tco (
    id_supuesto  BIGSERIAL     PRIMARY KEY,
    clave        VARCHAR(50)   NOT NULL,
    descripcion  VARCHAR(200)  NOT NULL,
    valor        NUMERIC(14,2) NOT NULL,
    unidad       VARCHAR(30)   NOT NULL,
    fuente       VARCHAR(200)  NOT NULL,
    fecha_fuente DATE          NOT NULL,
    CONSTRAINT uq_supuesto_tco_clave UNIQUE (clave),
    CONSTRAINT ck_supuesto_tco_valor CHECK (valor >= 0)
);

COMMENT ON TABLE supuesto_tco IS 'Supuestos por defecto del TCO; se editan con UPDATE, sin tocar código';
COMMENT ON COLUMN supuesto_tco.clave IS 'Identificador estable que usa el código para buscar el supuesto';
COMMENT ON COLUMN supuesto_tco.valor IS 'Valor en la unidad indicada (dinero en USD)';
COMMENT ON COLUMN supuesto_tco.fecha_fuente IS 'Fecha a la que corresponden los datos del supuesto';

-- Valores iniciales de ejemplo: confirmar y actualizar fuente y fecha cuando haya datos reales.
INSERT INTO supuesto_tco (clave, descripcion, valor, unidad, fuente, fecha_fuente) VALUES
    ('KM_POR_ANIO',        'Kilómetros recorridos por año',   15000.00, 'km/año',     'Valor inicial por defecto (por confirmar)', DATE '2026-11-09'),
    ('PRECIO_COMBUSTIBLE', 'Precio del combustible por galón',     3.80, 'USD/galón', 'Valor inicial por defecto (por confirmar)', DATE '2026-11-09'),
    ('SEGURO_ANUAL',       'Costo anual del seguro',             600.00, 'USD/año',   'Valor inicial por defecto (por confirmar)', DATE '2026-11-09'),
    ('GASTOS_ANUALES',     'Otros gastos anuales (mantenimiento, impuestos)', 400.00, 'USD/año', 'Valor inicial por defecto (por confirmar)', DATE '2026-11-09');
