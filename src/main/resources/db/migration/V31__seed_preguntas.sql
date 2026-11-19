-- E4-10-C: banco inicial de preguntas (5 por zona, 30 en total)
-- Cada pregunta se crea con su version 1 vigente.
-- Idempotente: si el codigo de la pregunta ya existe, no se vuelve a insertar.
-- costo_reparacion: valor referencial en USD.

WITH datos (zona, codigo, texto, gravedad, peso, explicacion, bandera_roja, costo) AS (
    VALUES
    -- ===== MOTOR =====
    ('MOTOR', 'MOT-01',
     '¿El motor enciende a la primera y mantiene un ralentí estable, sin vibraciones fuertes?',
     3, 1.50,
     'Un arranque difícil o un ralentí irregular puede indicar fallas de encendido, de inyección o desgaste interno.',
     FALSE, 120.00),
    ('MOTOR', 'MOT-02',
     '¿Sale humo azul, blanco denso o negro por el escape al acelerar o en ralentí?',
     4, 2.00,
     'El humo azul indica consumo de aceite, el blanco denso puede indicar entrada de refrigerante a la cámara de combustión y el negro una mezcla demasiado rica.',
     TRUE, 900.00),
    ('MOTOR', 'MOT-03',
     '¿Hay fugas visibles de aceite o refrigerante en el motor o debajo del vehículo?',
     3, 1.50,
     'Las fugas suelen venir de empaques, sellos o mangueras desgastadas; si se ignoran pueden terminar en sobrecalentamiento o falta de lubricación.',
     FALSE, 150.00),
    ('MOTOR', 'MOT-04',
     '¿El aceite del motor tiene aspecto lechoso o hay espuma bajo la tapa de llenado de aceite?',
     5, 3.00,
     'Un aceite lechoso sugiere mezcla con refrigerante, síntoma típico de empaque de culata dañado o culata fisurada; la reparación es costosa.',
     TRUE, 1200.00),
    ('MOTOR', 'MOT-05',
     '¿El indicador de temperatura sube más de lo normal o el motor se sobrecalienta en tráfico?',
     4, 2.50,
     'Puede deberse a falla del termostato, del radiador o del ventilador, pero también a un daño interno por un sobrecalentamiento previo.',
     FALSE, 300.00),

    -- ===== TRANSMISION =====
    ('TRANSMISION', 'TRA-01',
     '¿Los cambios entran de forma suave, sin golpes ni demoras, al subir y al bajar de marcha?',
     3, 1.50,
     'Los golpes o demoras al cambiar pueden indicar desgaste de sincronizadores, bajo nivel de aceite o fallas en el sistema de control.',
     FALSE, 450.00),
    ('TRANSMISION', 'TRA-02',
     'En caja manual, ¿el embrague patina o el punto de agarre del pedal es demasiado alto?',
     3, 1.50,
     'Un embrague que patina está al final de su vida útil; el reemplazo implica kit de embrague y mano de obra.',
     FALSE, 350.00),
    ('TRANSMISION', 'TRA-03',
     '¿Se escuchan zumbidos, ruidos metálicos o chasquidos al avanzar o al girar?',
     3, 1.25,
     'Los chasquidos al girar suelen venir de juntas homocinéticas desgastadas y los zumbidos, de rodamientos o del diferencial.',
     FALSE, 250.00),
    ('TRANSMISION', 'TRA-04',
     '¿Hay fugas de aceite en la caja o en los sellos de los ejes, o el nivel del líquido de transmisión está bajo?',
     3, 1.25,
     'Operar con poco lubricante acelera el desgaste interno de la caja y puede dejar el vehículo sin tracción.',
     FALSE, 180.00),
    ('TRANSMISION', 'TRA-05',
     'En caja automática, ¿el vehículo patina, da tirones o se queda en neutro al acelerar?',
     5, 3.00,
     'Es una de las fallas más costosas del vehículo: puede requerir reparación o reemplazo completo de la transmisión automática.',
     TRUE, 1800.00),

    -- ===== FRENOS =====
    ('FRENOS', 'FRE-01',
     '¿El pedal de freno se siente firme, sin recorrido excesivo ni sensación esponjosa?',
     5, 3.00,
     'Un pedal blando o que se hunde puede indicar aire o fugas en el circuito, o falla de la bomba de freno; es un riesgo directo de seguridad.',
     TRUE, 140.00),
    ('FRENOS', 'FRE-02',
     '¿Los frenos chillan o producen un roce metálico al frenar?',
     4, 2.00,
     'El chillido indica pastillas gastadas; el roce metálico significa que ya se está dañando el disco.',
     FALSE, 120.00),
    ('FRENOS', 'FRE-03',
     '¿El vehículo se desvía hacia un lado o vibra el volante al frenar?',
     4, 2.00,
     'El desvío puede venir de una pinza atascada o de desgaste desigual; la vibración suele indicar discos alabeados.',
     FALSE, 220.00),
    ('FRENOS', 'FRE-04',
     '¿Los discos o tambores presentan desgaste excesivo, ranuras profundas u óxido avanzado?',
     3, 1.50,
     'Los discos por debajo del espesor mínimo pierden capacidad de frenado y se sobrecalientan con facilidad.',
     FALSE, 180.00),
    ('FRENOS', 'FRE-05',
     '¿El freno de mano sostiene el vehículo en una pendiente?',
     3, 1.25,
     'Un freno de estacionamiento que no sostiene indica cables estirados o zapatas gastadas.',
     FALSE, 90.00),

    -- ===== SUSPENSION =====
    ('SUSPENSION', 'SUS-01',
     '¿Se escuchan golpes o traqueteos al pasar por baches o resaltos?',
     3, 1.50,
     'Suelen provenir de bujes, rótulas, terminales o soportes de amortiguador desgastados.',
     FALSE, 240.00),
    ('SUSPENSION', 'SUS-02',
     '¿Los amortiguadores tienen fugas de aceite, o la carrocería rebota más de una vez al presionarla?',
     3, 1.50,
     'Amortiguadores en mal estado alargan la distancia de frenado y desgastan los neumáticos de forma irregular.',
     FALSE, 380.00),
    ('SUSPENSION', 'SUS-03',
     '¿Los neumáticos muestran desgaste irregular, por ejemplo solo en un borde o con zonas disparejas?',
     3, 1.25,
     'El desgaste irregular apunta a problemas de alineación, de balanceo o de componentes de la suspensión.',
     FALSE, 150.00),
    ('SUSPENSION', 'SUS-04',
     '¿El volante se siente flojo o vibra, o el vehículo no mantiene la línea recta?',
     4, 2.00,
     'Puede indicar holgura en la dirección, problemas de alineación o componentes de suspensión desgastados.',
     FALSE, 260.00),
    ('SUSPENSION', 'SUS-05',
     '¿Los guardapolvos de las rótulas y de los terminales de dirección están rotos, o hay holgura en esas piezas?',
     5, 2.50,
     'Una rótula o terminal con holgura puede soltarse y hacer perder el control de la dirección; es un riesgo de seguridad.',
     TRUE, 200.00),

    -- ===== ELECTRICO =====
    ('ELECTRICO', 'ELE-01',
     '¿Quedan luces de advertencia encendidas en el tablero (motor, ABS, airbag, batería) después de arrancar?',
     4, 2.00,
     'Una luz que permanece encendida indica una falla registrada; conviene leer el código con un escáner antes de comprar.',
     FALSE, 180.00),
    ('ELECTRICO', 'ELE-02',
     '¿La batería arranca el motor con fuerza y se ve en buen estado, sin bornes sulfatados ni carcasa hinchada?',
     2, 1.00,
     'Una batería débil o hinchada hay que reemplazarla; es una reparación económica pero común.',
     FALSE, 120.00),
    ('ELECTRICO', 'ELE-03',
     '¿El alternador carga correctamente, con 13,5 a 14,5 V medidos en la batería con el motor encendido?',
     3, 1.50,
     'Un voltaje fuera de ese rango indica alternador o regulador defectuoso, que descarga la batería y deja el vehículo varado.',
     FALSE, 240.00),
    ('ELECTRICO', 'ELE-04',
     '¿Funcionan las luces, las direccionales, el limpiaparabrisas, los vidrios eléctricos y el aire acondicionado?',
     2, 1.00,
     'Las fallas en accesorios suelen ser de bajo costo, pero varias juntas pueden indicar problemas más amplios en el cableado.',
     FALSE, 150.00),
    ('ELECTRICO', 'ELE-05',
     '¿Hay cables expuestos, empalmes improvisados o señales de humedad en la caja de fusibles o en el cableado?',
     4, 2.50,
     'Los empalmes improvisados y la humedad generan cortocircuitos y son una causa frecuente de incendios en vehículos.',
     TRUE, 400.00),

    -- ===== CARROCERIA =====
    ('CARROCERIA', 'CAR-01',
     '¿Hay diferencias de tono o de textura en la pintura entre paneles que sugieran reparaciones por choque?',
     3, 1.50,
     'Los paneles repintados pueden ocultar choques previos; conviene revisar con luz natural y con un medidor de espesor de pintura.',
     FALSE, 400.00),
    ('CARROCERIA', 'CAR-02',
     '¿Las separaciones entre puertas, capó y baúl son uniformes en todo el contorno?',
     3, 1.50,
     'Las separaciones dispares indican que la carrocería fue golpeada o mal reparada.',
     FALSE, 450.00),
    ('CARROCERIA', 'CAR-03',
     '¿Hay óxido, corrosión o burbujas de pintura en el piso, los largueros, los pasos de rueda o los umbrales?',
     4, 2.50,
     'La corrosión en zonas estructurales compromete la resistencia del vehículo en un choque y es difícil de reparar bien.',
     TRUE, 650.00),
    ('CARROCERIA', 'CAR-04',
     '¿Hay señales de reparación estructural en el chasis, como soldaduras visibles, largueros doblados o cortes?',
     5, 3.00,
     'Un chasis reparado puede indicar un choque fuerte y afecta la seguridad y la alineación del vehículo; es una de las banderas rojas más importantes.',
     TRUE, 2000.00),
    ('CARROCERIA', 'CAR-05',
     '¿Los vidrios y el parabrisas están sin fisuras ni picaduras, y los sellos de las puertas no dejan entrar agua?',
     2, 1.00,
     'Las fisuras pueden extenderse y los sellos deteriorados provocan filtraciones y óxido en el piso.',
     FALSE, 130.00)
),
nuevas AS (
    INSERT INTO pregunta (id_zona, codigo)
    SELECT z.id_zona, d.codigo
    FROM datos d
    JOIN zona z ON z.codigo = d.zona
    ON CONFLICT (codigo) DO NOTHING
    RETURNING id_pregunta, codigo
)
INSERT INTO pregunta_version
    (id_pregunta, numero, texto, gravedad, peso, explicacion, bandera_roja, costo_reparacion, vigente)
SELECT n.id_pregunta, 1, d.texto, d.gravedad::smallint, d.peso, d.explicacion, d.bandera_roja, d.costo, TRUE
FROM nuevas n
JOIN datos d ON d.codigo = n.codigo;

-- Validacion: falla la migracion si algo del seed quedo incompleto
DO $$
DECLARE
    faltantes INT;
BEGIN
    SELECT count(*) INTO faltantes
    FROM zona z
    WHERE NOT EXISTS (
        SELECT 1
        FROM pregunta p
        JOIN pregunta_version v ON v.id_pregunta = p.id_pregunta AND v.vigente
        WHERE p.id_zona = z.id_zona AND p.activa
    );
    IF faltantes > 0 THEN
        RAISE EXCEPTION 'E4-10-C: % zona(s) sin preguntas', faltantes;
    END IF;

    SELECT count(*) INTO faltantes
    FROM pregunta p
    JOIN pregunta_version v ON v.id_pregunta = p.id_pregunta AND v.vigente
    WHERE p.codigo ~ '^(MOT|TRA|FRE|SUS|ELE|CAR)-0[1-5]$'
      AND (v.texto IS NULL OR v.texto = ''
           OR v.explicacion IS NULL OR v.explicacion = ''
           OR v.costo_reparacion IS NULL);
    IF faltantes > 0 THEN
        RAISE EXCEPTION 'E4-10-C: % pregunta(s) con campos incompletos', faltantes;
    END IF;
END $$;
