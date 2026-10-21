# Backend ChatConsultorio

API backend del proyecto, con PostgreSQL + pgvector corriendo en Docker.

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (incluye Docker Compose)
- Git

## Arranque rápido (base de datos)

1. Clona el repositorio y entra a la carpeta:

```bash
   git clone https://github.com/SamuraiPonchedefruta/backend-ChatConsultorio.git
   cd backend-ChatConsultorio
```

2. Crea tu archivo de variables de entorno a partir de la plantilla:

```bash
   cp .env.example .env
```

   Abre `.env` y cambia `POSTGRES_PASSWORD` por una contraseña propia.
   El archivo `.env` está en `.gitignore`: nunca se sube al repositorio.

3. Levanta la base de datos:

```bash
   docker compose up -d
```

4. Verifica que está lista:

```bash
   docker compose exec postgres pg_isready -U postgres -d autobot_db
```

   Debe responder `accepting connections`.

## Variables de entorno

| Variable | Descripción | Valor de ejemplo |
|---|---|---|
| `POSTGRES_DB` | Nombre de la base de datos | `autobot_db` |
| `POSTGRES_USER` | Usuario de PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL | *(defínela en tu `.env`)* |
| `POSTGRES_PORT` | Puerto expuesto en tu máquina | `5432` |

## Comandos útiles

```bash
docker compose ps            # ver el estado del contenedor
docker compose logs postgres # ver los logs de la base de datos
docker compose down          # detener el contenedor (conserva los datos)
docker compose down -v       # detener y BORRAR los datos (reinicio limpio)
```

## Notas importantes

- `scripts/schema.sql` se ejecuta **solo la primera vez**, cuando el volumen de
  datos está vacío. Si lo modificas o cambias la contraseña en `.env` después de
  haber levantado la base, ejecuta `docker compose down -v` y vuelve a levantarla.
- Si el puerto 5432 ya está ocupado en tu máquina, cambia `POSTGRES_PORT` en tu `.env`.
EOF
