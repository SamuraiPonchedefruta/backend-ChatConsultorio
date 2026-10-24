-- V4: primera versión de los textos legales que se aceptan al registrarse.
--
-- TEXTO PROVISIONAL: antes de abrir el registro a usuarios reales, publicar la redacción legal
-- definitiva como versión nueva (por ejemplo V5 con la versión 1.1). Esta fila no se puede
-- editar: el trigger de V3 lo impide, y así queda claro qué texto vio quien aceptó la 1.0.

INSERT INTO texto_legal (tipo, version, contenido, vigente_desde) VALUES
    ('TRATAMIENTO_DATOS', '1.0',
     '[TEXTO PROVISIONAL] Autorizo el tratamiento de mis datos personales (nombre, correo y celular) '
     'para crear y administrar mi cuenta y para prestar el servicio de consultoría.',
     now()),
    ('NOTIFICACIONES', '1.0',
     '[TEXTO PROVISIONAL] Autorizo recibir notificaciones y comunicaciones sobre el servicio '
     'por correo y por mensajes al celular. Puedo cambiar esta decisión en cualquier momento desde mi perfil.',
     now());
