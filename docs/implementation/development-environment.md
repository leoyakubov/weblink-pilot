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

For local development we use Mailpit.

The backend-local scripts load `backend/.env.local` first and then `backend/.env.smtp.local` if it exists. They also start the `mailpit` Docker service before launching the backend so the local mail catcher is available automatically.

If you want to start Mailpit manually, use:

```powershell
docker compose -f infra/docker-compose.yml up -d mailpit
```

```bash
docker compose -f infra/docker-compose.yml up -d mailpit
```

The local profile uses:

- SMTP host: `localhost`
- SMTP port: `1025`
- SMTP auth: `false`
- STARTTLS: `false`

The backend now performs a startup mail connection check in `local` and `dev`, so if Mailpit is not running you should see a clear startup failure or a `mail.server.health status=DOWN` log entry instead of discovering the problem only after the first email request.

For the demo environment we use Mailtrap Email Testing, so the team can inspect messages in the Mailtrap inbox and click the links without sending real mail to recipients.

The demo SMTP values should point to the Mailtrap inbox credentials and host:

- `sandbox.smtp.mailtrap.io`
- `587`

If you want to try the Mailtrap path locally for comparison, copy `backend/.env.smtp.local.example` to `backend/.env.smtp.local` and fill in the Mailtrap inbox credentials.

## Environment Files

- Keep backend-only values in `backend/.env.local`.
- Keep backend SMTP override values in `backend/.env.smtp.local`.
- Keep frontend-only values in `frontend/.env.local`.
- Keep deployment helper values in `infra/.env.local`.
- Keep Sonar helper values in `infra/sonar/.env.local`.
- Keep real secrets out of tracked files and git history.
- Document new required variables in `docs/operations/deployment.md` or the relevant feature doc.
- Use placeholder values only in tracked examples.

There is no shared repo-root `.env.local` now; use the area-specific files above instead.

### Config Source Map

| Runtime / workflow | Main config sources | Local override file | Dev / demo override source |
| --- | --- | --- | --- |
| Backend local | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-local.yml` | `backend/.env.local` | N/A |
| Backend dev | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-dev.yml` | `backend/.env.local` when running backend scripts manually or through the Docker stack | Docker Compose environment and `infra/.env.local` |
| Backend demo | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-demo.yml` | none | Render environment variables and GitHub deployment secrets/vars |
| Frontend local | `frontend/.env.local`, `frontend/vite.config.js` | `frontend/.env.local` | N/A |
| Frontend dev | `frontend/.env.local`, `frontend/vite.config.js` | `frontend/.env.local` | Docker Compose env or shell overrides |
| Frontend demo | GitHub Actions / Netlify build env | none | `VITE_API_BASE_URL` passed during Netlify deploy |
| Deploy smoke | `infra/.env.local` | `infra/.env.local` | GitHub Actions environment and repository secrets |
| Sonar local | `infra/sonar/.env.local` | `infra/sonar/.env.local` | GitHub Actions disabled for Sonar at the moment |

### Key Variables

| Variable or family | Read by | Source of truth |
| --- | --- | --- |
| `JWT_SECRET` / `APP_AUTH_JWT_SECRET` | Backend JWT signing | `backend/.env.local` for local runs; Render/GitHub secrets for demo |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | Backend GitHub OAuth | shell env, `backend/.env.local`, or Render secrets |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_SMTP_STARTTLS` | Backend mail sender | `application-local.yml`, `application-dev.yml`, `application-demo.yml` plus env overrides |
| `APP_AUTH_*` | Backend auth settings | `application.yml` plus env overrides |
| `BOOTSTRAP_*` | Backend demo seed accounts | `application.yml` and `application-demo.yml` plus demo env overrides |
| `APP_PUBLIC_BASE_URL` | Backend generated links | `application.yml` and profile overrides |
| `APP_SHORT_LINK_*` | Backend cleanup and expiry | `application.yml` and env overrides |
| `APP_CACHE_PROVIDER` | Backend cache mode | `application.yml` and profile overrides |
| `APP_RATE_LIMIT_*` | Backend rate limiting | `application.yml` and env overrides |
| `APP_CORS_ALLOWED_ORIGIN_*` / `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | Backend CORS allowlist | `application.yml` and `application-demo.yml` |
| `VITE_API_BASE_URL` | Frontend API client | `frontend/.env.local` for local runs, Netlify build env for demo |
| `VITE_DEV_SERVER_PORT` | Frontend Vite dev server | `frontend/.env.local` or the default from `frontend/vite.config.js` |
| `RENDER_DEPLOY_HOOK_URL`, `RENDER_API_KEY`, `RENDER_BACKEND_SERVICE_ID`, `RENDER_HEALTH_URL`, `FRONTEND_SMOKE_URL`, `NETLIFY_AUTH_TOKEN`, `NETLIFY_SITE_ID` | Deploy / smoke helpers | `infra/.env.local` |
| `SONAR_TOKEN`, `SONAR_HOST_URL` | Local Sonar helper | `infra/sonar/.env.local` |

Common environment values include:

- `APP_AUTH_ISSUER`
- `APP_AUTH_TOKEN_TTL_MINUTES`
- `APP_AUTH_REFRESH_TOKEN_TTL_DAYS`
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
