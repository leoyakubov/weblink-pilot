# Development Environment

This document describes the local workflow for building, testing, and maintaining WebLinkPilot.

## Baseline Tools

- Git
- Java 21
- Maven through the backend wrapper
- Node.js 24.16.0 LTS
- npm 11.13.0
- Docker Desktop or compatible Docker runtime
- PowerShell on Windows
- Bash-compatible shell on Unix/macOS/Linux

## Repository Layout

- `backend/` - Spring Boot modular backend.
- `frontend/` - Vue SPA.
- `infra/` - Docker, monitoring, and deployment support.
- `scripts/win/` - Windows helper scripts.
- `scripts/unix/` - Unix/macOS/Linux helper scripts.
- `docs/` - categorized SDLC documentation.

## Local Runtime

The recommended local flow is the Docker full stack.

Windows:

```powershell
.\scripts\win\dev\docker-full-stack.ps1
```

Unix/macOS/Linux:

```bash
./scripts/unix/dev/docker-full-stack.sh
```

The full stack includes:

| Service | Purpose | Default Port |
| --- | --- | --- |
| Postgres | relational database | `5432` |
| Redis | cache and refresh-session mirror | `6379` |
| Backend | Spring Boot API | `8080` |
| Frontend | Vue SPA | `8081` |

If you want monitoring locally, start it separately:

Windows:

```powershell
.\scripts\win\dev\monitoring.ps1
```

Unix/macOS/Linux:

```bash
./scripts/unix/dev/monitoring.sh
```

The monitoring stack includes:

| Service | Purpose | Default Port |
| --- | --- | --- |
| Prometheus | metrics scraping | `9090` |
| Grafana | dashboards | `3001` |

### Mail testing locally

The project uses Mailpit for local email testing.

If you run the full Docker stack, the backend container is already wired to Mailpit:

- SMTP host: `mailpit`
- SMTP port: `1025`
- Mailpit inbox UI: `http://localhost:8025`

If you run the backend on your machine and only want the SMTP catcher in Docker, start Mailpit on its own:

```powershell
docker compose -f infra/docker-compose.yml up -d mailpit
```

```bash
docker compose -f infra/docker-compose.yml up -d mailpit
```

Then point the backend to localhost Mailpit:

```powershell
$env:SPRING_MAIL_HOST = "localhost"
$env:SPRING_MAIL_PORT = "1025"
```

```bash
export SPRING_MAIL_HOST=localhost
export SPRING_MAIL_PORT=1025
```

Profile defaults:

- `local` uses `localhost:1025` for SMTP and `localhost:5173` for the frontend link target
- `dev` uses `mailpit:1025` for the Docker stack
- `demo` uses the external SMTP provider configured through environment variables

If you run the frontend outside Docker and still want local email links to open correctly, make sure the frontend runs on `http://localhost:5173`.
If you run `dev` outside Docker and still want Mailpit on the host, override the host and port to `localhost:1025`.

## Environment Files

- Keep developer-only values in `.env.local`.
- Keep real secrets out of tracked files and git history.
- Document new required variables in `docs/operations/deployment.md` or the relevant feature doc.
- Use placeholder values only in tracked examples.

Common environment values include:

- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- `FRONTEND_BASE_URL`
- `APP_PUBLIC_BASE_URL`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

## Development Commands

Start the backend only:

```powershell
.\scripts\win\dev\backend-local.ps1
```

```bash
./scripts/unix/dev/backend-local.sh
```

Start the frontend only:

```powershell
.\scripts\win\dev\frontend-local.ps1
```

```bash
./scripts/unix/dev/frontend-local.sh
```

Run local smoke checks:

```powershell
.\scripts\win\quality\deployment-smoke.ps1
```

```bash
./scripts/unix/quality/deployment-smoke.sh
```

## Verification Commands

Fast pre-push gate:

```powershell
.\scripts\win\run-before-push.ps1
```

```bash
./scripts/unix/run-before-push.sh
```

Backend checks:

```powershell
.\scripts\win\quality\backend-style.ps1
.\scripts\win\quality\backend-tests.ps1
```

```bash
./scripts/unix/quality/backend-style.sh
./scripts/unix/quality/backend-tests.sh
```

Frontend checks:

```powershell
.\scripts\win\quality\frontend-style.ps1
.\scripts\win\quality\frontend-tests.ps1
.\scripts\win\dev\frontend-build.ps1
```

```bash
./scripts/unix/quality/frontend-style.sh
./scripts/unix/quality/frontend-tests.sh
./scripts/unix/dev/frontend-build.sh
```

Manual coverage and security checks:

```powershell
.\scripts\win\quality\backend-coverage.ps1
.\scripts\win\quality\frontend-coverage.ps1
.\scripts\win\security\check-dependencies.ps1
```

```bash
./scripts/unix/quality/backend-coverage.sh
./scripts/unix/quality/frontend-coverage.sh
./scripts/unix/security/check-dependencies.sh
```

## Testing Policy

- Run the narrowest useful automated check while developing.
- Run the full pre-push gate before broad commits.
- For backend changes, prefer targeted Maven tests first, then the backend wrapper.
- For frontend changes, prefer focused Vitest tests first, then lint/build.
- For auth, links, analytics, monitoring, and email flows, update or use the matching guide in `docs/testing/`.
- If Docker or network access blocks a check, record the blocker and run the nearest local check.

## Documentation Policy

- Update `docs/planning/roadmap.md` when delivery scope changes.
- Update `docs/implementation/api-contract-v1.md` when backend/frontend contracts change.
- Update `docs/testing/feature-testing.md` when a user-visible flow changes.
- Update `docs/operations/deployment.md` when runtime variables, services, or demo deployment behavior changes.
- Update `docs/reference/security-review.md` when security posture changes.

## Commit Readiness

Before committing:

1. Check `git status --short`.
2. Run the relevant test/style/build commands.
3. Run `git diff --check`.
4. Update docs for durable behavior changes.
5. Use a concise Conventional Commit message.

Do not push without explicit user approval.
