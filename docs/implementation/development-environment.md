# Development Environment

This document describes the local workflow for building, testing, and maintaining WebLinkPilot.

## Baseline Tools

- Git
- Java 21
- Maven through the backend wrapper
- Node.js 24.16.0 LTS
- npm 11.13.0
- Docker Desktop or compatible Docker runtime
- WSL 2 with a Linux distribution on Windows
- Bash-compatible shell on Linux/macOS
- Docker Desktop WSL integration when developing on Windows

### Windows and WSL

Install WSL once from an elevated PowerShell terminal:

```powershell
wsl --install
```

Inside the WSL distribution, install Java 21, Node.js/npm, Git, and the small CLI tools used by the scripts (`curl`, `jq`, and `lsof`). Enable the distribution under Docker Desktop **Settings > Resources > WSL integration**.

PowerShell can invoke a script directly:

```powershell
wsl bash ./scripts/run-before-push.sh
```

Alternatively, enter WSL with `wsl`, open the repository, and use the Bash commands shown below. Keeping the clone in the WSL filesystem (for example `~/projects/weblink-pilot`) gives better filesystem performance than `/mnt/c/...`, but both layouts are supported.
If you reuse a Windows-created `frontend/node_modules` tree from WSL, the frontend scripts will refresh the frontend dependencies automatically the next time they run.
For Playwright-based browser tests in WSL, the scripts can drive a Windows Chrome/Edge executable through Playwright CDP if it is installed in the standard Windows location. You can still set `PLAYWRIGHT_BROWSER_PATH` to a Linux executable if you prefer a browser inside WSL.

## Repository Layout

- `backend/` - Spring Boot modular backend.
- `frontend/` - Vue SPA.
- `infra/` - Docker, monitoring, and deployment support.
- `scripts/` - shared Bash automation grouped into `dev`, `git`, `lib`, `quality`, and `security`.
- `docs/` - categorized SDLC documentation.

## Local Runtime

The recommended local flow is the Docker full stack.

Windows PowerShell:

```powershell
wsl bash ./scripts/dev/fullstack-dev.sh
```

WSL/Linux/macOS:

```bash
bash ./scripts/dev/fullstack-dev.sh
```

The full stack includes:

| Service | Purpose | Default Port |
| --- | --- | --- |
| Postgres | relational database | `5432` |
| Redis | cache and refresh-session mirror | `6379` |
| Backend | Spring Boot API | `8080` |
| Frontend | Vue SPA | `8081` |

If you want monitoring locally, start it separately:

Windows PowerShell:

```powershell
wsl bash ./scripts/dev/monitoring-stack.sh
```

WSL/Linux/macOS:

```bash
bash ./scripts/dev/monitoring-stack.sh
```

The monitoring stack includes:

| Service | Purpose | Default Port |
| --- | --- | --- |
| Prometheus | metrics scraping | `9090` |
| Grafana | dashboards | `3001` |

### Mail testing locally

The backend-local scripts load `backend/.env.local` first and then `backend/.env.smtp.local` if it exists. They also start the `mailpit` Docker service before launching the backend so the local mail catcher is available automatically.

| Profile | Mail mode | SMTP host | SMTP auth | From address | Notes |
| --- | --- | --- | --- | --- | --- |
| `local` | `SMTP` | `localhost:1025` | `false` | `SPRING_MAIL_FROM_ADDRESS` | Mailpit in Docker or a manual local Mailpit instance. |
| `dev` | `SMTP` | `mailpit:1025` | `false` | `SPRING_MAIL_FROM_ADDRESS` | Docker Compose Mailpit service. |
| `local demo` | `SMTP` | `smtp-relay.brevo.com:587` | `true` | `SPRING_MAIL_FROM_ADDRESS` | Use Brevo credentials from `backend/.env.smtp.local`. |
| `demo` (Render) | `SMTP` | `smtp-relay.brevo.com:587` | `true` | `SPRING_MAIL_FROM_ADDRESS` | Render demo with Brevo SMTP and a verified sender. |

The backend performs a startup mail connection check in `local` and `dev`, so if Mailpit is not running you should see a clear startup failure or a `mail.server.health status=DOWN` log entry instead of discovering the problem only after the first email request.

If you ever need the old preview-mailbox behavior again for a one-off test, set `APP_AUTH_MAIL_DELIVERY_MODE=DEMO_PREVIEW` manually before starting the backend. The switch is still available, but the default demo path now uses SMTP.

### Demo-like local run

If you want to test the app locally the same way demo behaves on Render, use the demo-local launcher with Brevo SMTP credentials instead of the Mailpit flow:

Windows PowerShell:

```powershell
wsl bash ./scripts/dev/fullstack-demo-local.sh
wsl bash ./scripts/dev/frontend-demo-local.sh
```

WSL/Linux/macOS:

```bash
bash ./scripts/dev/fullstack-demo-local.sh
bash ./scripts/dev/frontend-demo-local.sh
```

This flow starts:

- PostgreSQL
- Redis
- backend in `demo` profile
- frontend in production-like preview mode on `http://localhost:8081`

In this mode:

- the backend sends real verification and password reset emails through Brevo
- the frontend shows the regular success flow and the email opens in your real inbox
- Mailpit is not used

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
| `SPRING_MAIL_FROM_ADDRESS`, `SPRING_MAIL_FROM_NAME` | Backend mail sender identity | `application.yml` plus env overrides |
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
wsl bash ./scripts/dev/backend-local.sh
```

```bash
bash ./scripts/dev/backend-local.sh
```

Start the frontend only:

```powershell
wsl bash ./scripts/dev/frontend-local.sh
```

```bash
bash ./scripts/dev/frontend-local.sh
```

Run local smoke checks:

```powershell
wsl bash ./scripts/quality/deployment-smoke.sh
```

```bash
bash ./scripts/quality/deployment-smoke.sh
```

## Verification Commands

Fast pre-push gate:

```powershell
wsl bash ./scripts/run-before-push.sh
```

```bash
bash ./scripts/run-before-push.sh
```

Backend checks:

```powershell
wsl bash ./scripts/quality/backend-style.sh
wsl bash ./scripts/quality/backend-tests.sh
```

```bash
bash ./scripts/quality/backend-style.sh
bash ./scripts/quality/backend-tests.sh
```

Frontend checks:

```powershell
wsl bash ./scripts/quality/frontend-style.sh
wsl bash ./scripts/quality/frontend-tests.sh
wsl bash ./scripts/dev/frontend-build.sh
```

```bash
bash ./scripts/quality/frontend-style.sh
bash ./scripts/quality/frontend-tests.sh
bash ./scripts/dev/frontend-build.sh
```

Manual coverage and security checks:

```powershell
wsl bash ./scripts/quality/backend-coverage.sh
wsl bash ./scripts/quality/frontend-coverage.sh
wsl bash ./scripts/security/check-dependencies.sh
```

```bash
bash ./scripts/quality/backend-coverage.sh
bash ./scripts/quality/frontend-coverage.sh
bash ./scripts/security/check-dependencies.sh
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
