# Infrastructure

This folder contains the Docker Compose stack for running the full project locally:

- PostgreSQL 17
- backend service
- frontend service
- Prometheus and Grafana are available in the optional monitoring stack

## Requirements

- Docker Desktop running
- Docker Compose available from the terminal

## Start

From the repository root:

```bash
bash ./scripts/dev/fullstack-dev.sh
```

From Windows PowerShell, run `wsl bash ./scripts/dev/fullstack-dev.sh`.

Or run Compose directly:

```powershell
docker compose -p weblink-pilot -f infra/docker-compose.yml up --build
```

## Stop

```powershell
docker compose -p weblink-pilot -f infra/docker-compose.yml down
```

## URLs

- Frontend: `http://localhost:8081`
- Backend API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Monitoring stack: `docker compose -p weblink-pilot-monitoring -f infra/monitoring/docker-compose.monitoring.yml up --build`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

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

### Environment files

- `backend/.env.local`: local backend secret values such as `JWT_SECRET`
- `infra/.env.local`: deployment, smoke, and Netlify/Render helper values
- `infra/sonar/.env.local`: Sonar token and local Sonar host URL

## Notes

- The Docker stack uses PostgreSQL, while the local script-based backend run still uses the local development configuration.
- For day-to-day development, the Docker stack is the closest preview of the deployed setup.
- The frontend proxy exposes backend actuator endpoints so the admin monitoring page can open live health and metrics links from the same origin.
