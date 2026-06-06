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
- `infra/` - Docker, Prometheus, Grafana, and deployment support.
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
| Prometheus | metrics scraping | `9090` |
| Grafana | dashboards | `3001` |
| Frontend | Vue SPA | `8081` |

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
