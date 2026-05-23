# WebLinkPilot

Modern URL shortening platform with QR codes, analytics, and a mobile-first web UI.

## Docs

- [Architecture Plan](docs/architecture-plan.md)
- [Product Spec](docs/product-spec.md)
- [Backend Module Plan](docs/backend-module-plan.md)
- [Frontend Plan](docs/frontend-plan.md)
- [Architecture Decisions](docs/adr.md)
- [Tech Stack](docs/tech-stack.md)
- [API Contract v1](docs/api-contract-v1.md)
- [Backend Testing Strategy](docs/backend-testing.md)
- [Implementation Checklist](docs/implementation-checklist.md)

## Repository Structure

- `backend/` - Java modular monolith and infrastructure
- `frontend/` - Vue mobile-first web application
- `docs/` - architecture, product, roadmap, and decisions
- `infra/` - Docker, deployment, and local environment tooling

## Backend Quick Start

Requires Java 21. Maven is downloaded automatically by the wrapper on the first run.

From `backend/` on Windows:

```powershell
.\mvnw.cmd -pl app -am clean package -DskipTests
java -jar app\target\app-0.1.0-SNAPSHOT.jar
```

On macOS/Linux:

```bash
./mvnw -pl app -am clean package -DskipTests
java -jar app/target/app-0.1.0-SNAPSHOT.jar
```

Useful API endpoints after startup:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/r/{code}`
- `http://localhost:8080/api/v1/urls/{code}/preview`
- `http://localhost:8080/api/v1/urls/{code}/qr`
- `http://localhost:8080/api/v1/analytics/{code}`
- `http://localhost:8080/api/v1/analytics/{code}/count`

## Backend Coverage

To generate the aggregate backend coverage report from `backend/`:

```powershell
.\mvnw.cmd -pl coverage -am verify
```

On macOS/Linux:

```bash
./mvnw -pl coverage -am verify
```

The HTML report is written to `backend/coverage/target/site/jacoco-aggregate/index.html`.

## Run Scripts

From the repo root, you can start the apps with:

- Windows backend: [`scripts/run-backend.ps1`](scripts/run-backend.ps1)
- Unix backend: [`scripts/run-backend.sh`](scripts/run-backend.sh)
- Windows frontend: [`scripts/run-frontend.ps1`](scripts/run-frontend.ps1)
- Unix frontend: [`scripts/run-frontend.sh`](scripts/run-frontend.sh)
- Windows backend tests: [`scripts/test-backend.ps1`](scripts/test-backend.ps1)
- Unix backend tests: [`scripts/test-backend.sh`](scripts/test-backend.sh)

The backend script uses the Maven wrapper and the frontend script installs dependencies on first run if `node_modules/` is missing.

To run the whole Docker stack from the repo root:

- Windows: [`scripts/run-docker.ps1`](scripts/run-docker.ps1)
- Unix: [`scripts/run-docker.sh`](scripts/run-docker.sh)

For a faster backend dev loop, use the dev launcher instead:

- Windows backend dev: [`scripts/run-backend-dev.ps1`](scripts/run-backend-dev.ps1)
- Unix backend dev: [`scripts/run-backend-dev.sh`](scripts/run-backend-dev.sh)

Note: stop any already running backend instance before starting dev mode, otherwise port `8080` will already be in use.

## Docker Stack

The repo also includes a containerized local stack:

- [`infra/docker-compose.yml`](infra/docker-compose.yml)
- backend image built from [`backend/Dockerfile`](backend/Dockerfile)
- frontend image built from [`frontend/Dockerfile`](frontend/Dockerfile)
- Postgres 17 for persistence

Start it from the repo root:

```bash
docker compose -f infra/docker-compose.yml up --build
```

Services:

- frontend: `http://localhost:8081`
- backend API: `http://localhost:8080/api/v1`
- backend direct: `http://localhost:8080`

The frontend container serves the Vue app through nginx and proxies API and redirect requests to the backend. The backend uses Postgres in this setup, while local scripts still use the in-memory H2 profile.

For a lightweight browser smoke check against the Docker stack, run the frontend script:

```bash
npm run smoke:docker
```

It expects the Docker stack to be up and a local Chrome or Edge executable to be available, or `PLAYWRIGHT_BROWSER_PATH` to be set.

## Frontend Quick Start

Requires Node.js 22+.

From `frontend/`:

```bash
npm install
npm run dev
```

The app expects the backend at `http://localhost:8080/api/v1` by default. You can override that with a local `.env` file:

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Default dev credentials for the current backend:

- username: `admin`
- password: `admin123`
