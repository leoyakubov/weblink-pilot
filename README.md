# WebLinkPilot

[![CI](https://github.com/leoyakubov/weblink-pilot/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leoyakubov/weblink-pilot/actions/workflows/ci.yml)
[![Frontend Deploy](https://api.netlify.com/api/v1/badges/96caf667-74c1-4b10-829d-f9af82d694d5/deploy-status)](https://app.netlify.com/sites/96caf667-74c1-4b10-829d-f9af82d694d5/deploys)
[![Backend Deploy](https://img.shields.io/badge/Render-backend-46E3B7?logo=render&logoColor=white)](https://dashboard.render.com/)

Modern URL shortening platform with QR codes, separate redirect vs QR analytics, and a mobile-first web UI.

Live demo: Netlify frontend and Render backend with Render Postgres and Render Key Value Redis.

The app supports both anonymous demo links and signed-in user-owned links. Guests can shorten URLs immediately, while authenticated users get owned links, private history, and admin-only monitoring if they have the admin role.
The cleaner SaaS-style landing page keeps the main create flow front and center, while the About page now holds the tech stack and developer settings.
Local/dev startup also seeds the shared `admin` and `user` accounts plus a small set of starter links so the dashboards are never empty on first run.

## Docs

- [Architecture Plan](docs/architecture-plan.md)
- [Product Spec](docs/product-spec.md)
- [Backend Module Plan](docs/backend-module-plan.md)
- [Frontend Plan](docs/frontend-plan.md)
- [Architecture Decisions](docs/adr.md)
- [Tech Stack](docs/tech-stack.md)
- [API Contract v1](docs/api-contract-v1.md)
- [Backend Testing Strategy](docs/backend-testing.md)
- [Development Standards](docs/development-standards.md)
- [Deployment](docs/deployment.md)
- [Roadmap](docs/roadmap.md)

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

## Backend Quality

To run the backend verification gate from `backend/`:

```powershell
.\mvnw.cmd -Pci clean install
```

On macOS/Linux:

```bash
./mvnw -Pci clean install
```

The HTML report is written to `backend/coverage/target/site/jacoco-aggregate/index.html`.

Helper script:

- Windows: [`scripts/backend/check-coverage.ps1`](scripts/backend/check-coverage.ps1)
- Unix: [`scripts/backend/check-coverage.sh`](scripts/backend/check-coverage.sh)

## Dependency Security

Run the dependency vulnerability checks from the repo root when you want the manual security gate:

```powershell
.\scripts\check-dependencies.ps1
```

On macOS/Linux:

```bash
./scripts/check-dependencies.sh
```

Backend-only check:

- Windows: [`scripts/backend/check-vulnerabilities.ps1`](scripts/backend/check-vulnerabilities.ps1)
- Unix: [`scripts/backend/check-vulnerabilities.sh`](scripts/backend/check-vulnerabilities.sh)

Frontend-only check:

- Windows: [`scripts/frontend/check-vulnerabilities.ps1`](scripts/frontend/check-vulnerabilities.ps1)
- Unix: [`scripts/frontend/check-vulnerabilities.sh`](scripts/frontend/check-vulnerabilities.sh)

## Secret Scanning

Run the repo-wide secret scan from the repo root:

```powershell
.\scripts\check-secrets.ps1
```

On macOS/Linux:

```bash
./scripts/check-secrets.sh
```

The scan uses the official Gitleaks Docker image and the repo-level [`gitleaks`](.gitleaks.toml) configuration.
It scans Git-tracked content, so your local `.env.local` stays private to your machine and does not block the repo gate.

## SonarQube / Code Quality

Local SonarQube support is available through the Docker stack in `infra/sonar/`.
GitHub Actions Sonar is currently disabled for now; run Sonar locally with the helper scripts below.

Start it from the repo root:

```powershell
.\scripts\sonar\run-sonar-stack.ps1
```

On macOS/Linux:

```bash
./scripts/sonar/run-sonar-stack.sh
```

Then run analysis from `backend/`:

```powershell
$env:SONAR_TOKEN = "<your-token>"
$env:SONAR_HOST_URL = "http://localhost:9001"
.\mvnw.cmd -Pci clean install sonar:sonar -Dsonar.token=$env:SONAR_TOKEN -Dsonar.host.url=$env:SONAR_HOST_URL
```

On macOS/Linux:

```bash
export SONAR_TOKEN="<your-token>"
export SONAR_HOST_URL="http://localhost:9001"
./mvnw -Pci clean install sonar:sonar -Dsonar.token="$SONAR_TOKEN" -Dsonar.host.url="$SONAR_HOST_URL"
```

The default local SonarQube UI is available at `http://localhost:9001`.

If you prefer not to type the Maven command manually, use the helper scripts:

- Windows: [`scripts/sonar/run-sonar-analysis.ps1`](scripts/sonar/run-sonar-analysis.ps1)
- Unix: [`scripts/sonar/run-sonar-analysis.sh`](scripts/sonar/run-sonar-analysis.sh)

For a local-only convenience file, create `.env.local` at the repo root with:

```bash
SONAR_TOKEN=your-token-here
SONAR_HOST_URL=http://localhost:9001
JWT_SECRET=your-local-jwt-secret
```

## Run Scripts

From the repo root, the preferred quick-run entrypoints are grouped by area:

- Backend local: [`scripts/backend/local-run-backend.ps1`](scripts/backend/local-run-backend.ps1)
- Backend dev only: [`scripts/backend/dev-run-backend-only.ps1`](scripts/backend/dev-run-backend-only.ps1)
- Backend format: [`scripts/backend/format-backend.ps1`](scripts/backend/format-backend.ps1)
- Backend style check: [`scripts/backend/check-style.ps1`](scripts/backend/check-style.ps1)
- Backend tests only: [`scripts/backend/test-backend.ps1`](scripts/backend/test-backend.ps1)
- Backend quality (tests + coverage): [`scripts/backend/check-coverage.ps1`](scripts/backend/check-coverage.ps1)
- Backend vulnerability check: [`scripts/backend/check-vulnerabilities.ps1`](scripts/backend/check-vulnerabilities.ps1)
- Frontend local: [`scripts/frontend/local-run-frontend.ps1`](scripts/frontend/local-run-frontend.ps1)
- Frontend format: [`scripts/frontend/format-frontend.ps1`](scripts/frontend/format-frontend.ps1)
- Frontend style check: [`scripts/frontend/check-style.ps1`](scripts/frontend/check-style.ps1)
- Frontend tests and coverage: [`scripts/frontend/test-frontend.ps1`](scripts/frontend/test-frontend.ps1)
- Frontend vulnerability check: [`scripts/frontend/check-vulnerabilities.ps1`](scripts/frontend/check-vulnerabilities.ps1)
- Frontend coverage gate: `npm run test:coverage` from `frontend/`
- Frontend smoke test: [`scripts/frontend/smoke-frontend.ps1`](scripts/frontend/smoke-frontend.ps1)
- Dependency security: [`scripts/check-dependencies.ps1`](scripts/check-dependencies.ps1)
- Secret scanning: [`scripts/check-secrets.ps1`](scripts/check-secrets.ps1)
- Dev Docker stack: [`scripts/docker/dev-run-docker.ps1`](scripts/docker/dev-run-docker.ps1)
- SonarQube stack: [`scripts/sonar/run-sonar-stack.ps1`](scripts/sonar/run-sonar-stack.ps1)
- Sonar analysis: [`scripts/sonar/run-sonar-analysis.ps1`](scripts/sonar/run-sonar-analysis.ps1)
- Git hook setup: [`scripts/setup-git-hooks.ps1`](scripts/setup-git-hooks.ps1)
- Before push check: [`scripts/check-before-push.ps1`](scripts/check-before-push.ps1)

Unix versions live beside them with the same names ending in `.sh`.

To disable the pre-push hook in the current clone, run:

```powershell
git config --unset core.hooksPath
```

To enable it again:

```powershell
.\scripts\setup-git-hooks.ps1
```

The original flat scripts still exist for compatibility, but the grouped ones are easier to scan and tab-complete.

Note: stop any already running backend instance before starting dev mode, otherwise port `8080` will already be in use.

If you want the exact test/build shortcuts the project uses day to day:

- Frontend tests: [`scripts/frontend/test-frontend.ps1`](scripts/frontend/test-frontend.ps1)
- Frontend build: [`scripts/frontend/build-frontend.ps1`](scripts/frontend/build-frontend.ps1)
- Frontend coverage: [`scripts/frontend/check-coverage.ps1`](scripts/frontend/check-coverage.ps1)
- Frontend smoke test: [`scripts/frontend/smoke-frontend.ps1`](scripts/frontend/smoke-frontend.ps1)

## Docker Stack

The repo also includes a containerized local stack:

- [`infra/docker-compose.yml`](infra/docker-compose.yml)
- backend image built from [`backend/Dockerfile`](backend/Dockerfile)
- frontend image built from [`frontend/Dockerfile`](frontend/Dockerfile)
- Postgres 17 for persistence
- Redis 7 for hot-cache lookups and analytics cache invalidation

Start it from the repo root:

```bash
docker compose -f infra/docker-compose.yml up --build
```

Services:

- frontend: `http://localhost:8081`
- backend API: `http://localhost:8080/api/v1`
- backend direct: `http://localhost:8080`

The frontend container serves the Vue app through nginx and proxies API and redirect requests to the backend. The Docker stack uses the `dev` Spring profile with PostgreSQL and Redis so it behaves like a production-shaped local stack, while direct local development still uses the `local` profile with in-memory H2.

### Backend Profiles

- `local`: default for developer workflows, uses H2 and localhost origins
- `dev`: Docker stack profile, uses PostgreSQL, Redis, and localhost deployment wiring
- `demo`: use for deployed demo instances, uses PostgreSQL, Redis, and runtime secrets
- `ci`: verification profile for automated builds, tests, coverage, and static analysis

When running `local`, the H2 console is available at `http://localhost:8080/h2-console`.

You can also select the Maven convenience profiles when running the backend directly:

```powershell
.\mvnw.cmd -Plocal -pl app -am spring-boot:run
.\mvnw.cmd -Pdev -pl app -am spring-boot:run
.\mvnw.cmd -Pdemo -pl app -am spring-boot:run
```

Quick guide:

- `local`: [`scripts/backend/local-run-backend.ps1`](scripts/backend/local-run-backend.ps1)
- `dev`: [`scripts/docker/dev-run-docker.ps1`](scripts/docker/dev-run-docker.ps1) for the full stack, or [`scripts/backend/dev-run-backend-only.ps1`](scripts/backend/dev-run-backend-only.ps1) after Postgres and Redis are up locally
- `demo`: Render backend + Netlify frontend

For a lightweight browser smoke check against the Docker stack, use:

- Windows: [`scripts/frontend/smoke-frontend.ps1`](scripts/frontend/smoke-frontend.ps1)
- Unix: [`scripts/frontend/smoke-frontend.sh`](scripts/frontend/smoke-frontend.sh)

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

Default local/dev credentials for the current backend:

- `admin / admin123`
- `user / user123`

Demo deployments can seed the same accounts through the `BOOTSTRAP_*` environment variables listed in the deployment docs.

Guest mode does not require signing in and is the fastest way to create a demo link.
