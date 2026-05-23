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

## Run Scripts

From the repo root, you can start the apps with:

- Windows backend: [`scripts/run-backend.ps1`](scripts/run-backend.ps1)
- Unix backend: [`scripts/run-backend.sh`](scripts/run-backend.sh)
- Windows frontend: [`scripts/run-frontend.ps1`](scripts/run-frontend.ps1)
- Unix frontend: [`scripts/run-frontend.sh`](scripts/run-frontend.sh)

The backend script uses the Maven wrapper and the frontend script installs dependencies on first run if `node_modules/` is missing.

For a faster backend dev loop, use the dev launcher instead:

- Windows backend dev: [`scripts/run-backend-dev.ps1`](scripts/run-backend-dev.ps1)
- Unix backend dev: [`scripts/run-backend-dev.sh`](scripts/run-backend-dev.sh)

Note: stop any already running backend instance before starting dev mode, otherwise port `8080` will already be in use.

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
