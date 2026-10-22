# Ambientes

| Ambiente | Perfil Spring | Dónde corre | Base de datos |
|---|---|---|---|
| Local | `local` (por defecto) | Máquina del desarrollador | PostgreSQL en Docker Compose (`autobot_db`) |
| Staging | `staging` | Render (Web Service) | PostgreSQL de Render, exclusiva de staging |
| Prod | `prod` | Por definir | Por definir |

## Variables de entorno

Los secretos **nunca** se escriben en el repositorio. Local las lee de `.env`
(ver `.env.example`); staging y prod las reciben de la plataforma.

| Variable | Obligatoria en staging/prod | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sí | `staging` o `prod` |
| `POSTGRES_HOST` | Sí | Host de la base de datos |
| `POSTGRES_PORT` | No (por defecto 5432) | Puerto |
| `POSTGRES_DB` | Sí | Nombre de la base |
| `POSTGRES_USER` | Sí | Usuario |
| `POSTGRES_PASSWORD` | Sí | Contraseña (secreto) |
| `PORT` | La inyecta Render | Puerto HTTP |

Si falta una variable obligatoria, la app no arranca. El error aparece como
`UnknownHostException: ${NOMBRE}`: el nombre entre llaves es la variable que falta.

## Staging en Render

1. Crear una base **PostgreSQL** en Render (misma región que el servicio).
2. Crear un **Web Service** desde este repositorio, rama `main`, entorno **Docker**.
3. Configurar en *Environment* las variables de la tabla anterior, usando el host
   **interno** de la base de datos.
4. En *Health Check Path*, poner `/actuator/health`.
5. Activar *Auto-Deploy* sobre `main`.

## Verificar un despliegue

- Logs: `The following 1 profile is active: "staging"` y `Successfully applied N migration`.
- `GET <url-de-staging>/actuator/health` responde `{"status":"UP", ...}`.
- La tabla `flyway_schema_history` existe en la base de staging y no en la de otros ambientes.

## Cambiar de ambiente

No se modifica código: solo cambian `SPRING_PROFILES_ACTIVE` y las variables de
conexión del servicio. Un redeploy aplica el cambio.
