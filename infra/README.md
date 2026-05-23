# Infrastructure

This folder contains the Docker Compose stack for running the full project locally:

- PostgreSQL 17
- backend service
- frontend service

## Requirements

- Docker Desktop running
- Docker Compose available from the terminal

## Start

From the repository root:

```powershell
.\scripts\docker\run-docker.ps1
```

```bash
./scripts/docker/run-docker.sh
```

Or run Compose directly:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

## Stop

```powershell
docker compose -f infra/docker-compose.yml down
```

## URLs

- Frontend: `http://localhost:8081`
- Backend API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Common issues

### Docker daemon is not running

If you see an error about `docker_engine` or a missing pipe, start Docker Desktop first and wait until the daemon is ready.

### Port already in use

If `8080`, `8081`, or `5432` are already occupied, stop the previous stack or free the port before starting Compose again.

### Backend does not start after a database change

Rebuild the stack with `--build` so the backend image picks up the latest code and dependencies.

### CORS error in the browser

The frontend should call the backend through the Compose proxy using `/api/v1`.

If you changed the frontend API base URL manually, make sure it still points to the same origin or update the backend CORS allowlist accordingly.

## Notes

- The Docker stack uses PostgreSQL, while the local script-based backend run still uses the local development configuration.
- For day-to-day development, the Docker stack is the closest preview of the deployed setup.
