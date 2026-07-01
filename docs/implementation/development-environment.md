# Development Environment

This document describes the local workflow for building, testing, and maintaining WeblinkPilot.

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

Inside the WSL distribution, install Java 21, Node.js 24.16.0 LTS, npm 11.13.0, Git, and the small CLI tools used by the scripts (`curl`, `jq`, and `lsof`). Enable the distribution under Docker Desktop **Settings > Resources > WSL integration**.

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

| Service  | Purpose                          | Default Port |
| -------- | -------------------------------- | ------------ |
| Postgres | relational database              | `5432`       |
| Redis    | cache and refresh-session mirror | `6379`       |
| Backend  | Spring Boot API                  | `8080`       |
| Frontend | Vue SPA                          | `8081`       |

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

| Service    | Purpose          | Default Port |
| ---------- | ---------------- | ------------ |
| Prometheus | metrics scraping | `9090`       |
| Grafana    | dashboards       | `3001`       |

### Local observability security

The backend protects operational endpoints differently depending on the active profile.
This keeps demo and production-style runs safer while still allowing the local Docker monitoring stack to scrape metrics.

| Profile | `APP_SECURITY_PUBLIC_OBSERVABILITY` default | `/actuator/health` and `/actuator/info` | `/actuator/metrics` and `/actuator/prometheus` |
| ------- | ------------------------------------------- | --------------------------------------- | ---------------------------------------------- |
| `local` | `true`                                      | Public                                  | Public, so local Prometheus can scrape         |
| `dev`   | `true`                                      | Public                                  | Public, so Docker Prometheus can scrape        |
| `demo`  | `false`                                     | Public                                  | Requires admin access                          |
| `test`  | `false`                                     | Public                                  | Requires admin access                          |
| default | `false`                                     | Public                                  | Requires admin access                          |

`APP_SECURITY_PUBLIC_OBSERVABILITY` controls only the operational actuator metrics endpoints.
It does not make application admin APIs public.
Leave it as `false` for demo and production-style deployments unless metrics are protected by a separate network boundary.

The local monitoring stack scrapes:

```yaml
metrics_path: /actuator/prometheus
```

If Prometheus shows no backend metrics locally, check:

- the backend is running with the `local` or `dev` profile
- `APP_SECURITY_PUBLIC_OBSERVABILITY` was not overridden to `false`
- the backend is reachable from the Prometheus container as `backend:8080`

### Mail testing locally

The backend-local and backend-dev scripts load `backend/.env`, then force Mailpit SMTP settings after the file is loaded. This keeps local/dev email safe even when `backend/.env` also contains Brevo credentials for demo-local runs.

| Profile         | Mail mode | SMTP host                  | SMTP auth | From address               | Notes                                                 |
| --------------- | --------- | -------------------------- | --------- | -------------------------- | ----------------------------------------------------- |
| `local`         | `SMTP`    | `localhost:1025`           | `false`   | `SPRING_MAIL_FROM_ADDRESS` | Mailpit in Docker or a manual local Mailpit instance. |
| `dev`           | `SMTP`    | `mailpit:1025`             | `false`   | `SPRING_MAIL_FROM_ADDRESS` | Docker Compose Mailpit service.                       |
| `local demo`    | `SMTP`    | `smtp-relay.brevo.com:587` | `true`    | `SPRING_MAIL_FROM_ADDRESS` | Use Brevo credentials from `backend/.env`.            |
| `demo` (Render) | `SMTP`    | `smtp-relay.brevo.com:587` | `true`    | `SPRING_MAIL_FROM_ADDRESS` | Render demo with Brevo SMTP and a verified sender.    |

The backend performs a startup mail connection check in `local` and `dev`, so if Mailpit is not running you should see a clear startup failure or a `mail.server.health status=DOWN` log entry instead of discovering the problem only after the first email request.

If you ever need the old preview-mailbox behavior again for a one-off test, set `APP_AUTH_MAIL_DELIVERY_MODE=DEMO_PREVIEW` manually before starting the backend. The switch is still available, but the default demo path now uses SMTP.

### Demo-like local run

If you want to test the app locally the same way demo behaves on Render, use the demo-local launcher with Brevo SMTP credentials instead of the Mailpit flow. Do not put Brevo host/port overrides into local/dev scripts; those profiles are intentionally pinned to Mailpit.

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

- Keep backend-only values in `backend/.env`. Start from `backend/.env.example`.
- Keep backend Brevo credentials in `backend/.env` for demo/demo-local only. Local/dev scripts force Mailpit after loading this file.
- Keep frontend-only values in `frontend/.env`. Start from `frontend/.env.example`.
- Keep deployment helper values in `infra/.env`. Start from `infra/.env.example`.
- Keep Sonar helper values in `infra/sonar/.env`. Start from `infra/sonar/.env.example`.
- Keep real secrets out of tracked files and git history.
- Document new required variables in `docs/operations/deployment.md` or the relevant feature doc.
- Use placeholder values only in tracked examples.

There is no shared repo-root `.env` now; use the area-specific files above instead.

### Config Source Map

| Runtime / workflow | Main config sources                                                                                                      | Local override file                                                              | Dev / demo override source                                      |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Backend local      | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-local.yml` | `backend/.env`                                                                   | N/A                                                             |
| Backend dev        | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-dev.yml`   | `backend/.env` when running backend scripts manually or through the Docker stack | Docker Compose environment and `infra/.env`                     |
| Backend demo       | `backend/application/src/main/resources/application.yml`, `backend/application/src/main/resources/application-demo.yml`  | none                                                                             | Render environment variables and GitHub deployment secrets/vars |
| Frontend local     | `frontend/.env`, `frontend/vite.config.js`                                                                               | `frontend/.env`                                                                  | N/A                                                             |
| Frontend dev       | `frontend/.env`, `frontend/vite.config.js`                                                                               | `frontend/.env`                                                                  | Docker Compose env or shell overrides                           |
| Frontend demo      | GitHub Actions / Netlify build env                                                                                       | none                                                                             | `VITE_API_BASE_URL` passed during Netlify deploy                |
| Deploy smoke       | `infra/.env`                                                                                                             | `infra/.env`                                                                     | GitHub Actions environment and repository secrets               |
| Sonar local        | `infra/sonar/.env`                                                                                                       | `infra/sonar/.env`                                                               | GitHub Actions disabled for Sonar at the moment                 |

### Key Variables

| Variable or family                                                                                                                                          | Read by                      | Source of truth                                                                                                                                                                                                                                             |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `JWT_SECRET` / `APP_AUTH_JWT_SECRET`                                                                                                                        | Backend JWT signing          | `backend/application/src/main/resources/application.yml` reads `APP_AUTH_JWT_SECRET` first, then `JWT_SECRET`; `application-local.yml` and `application-dev.yml` add a non-blank local fallback; `backend/.env` is the convenient place to set either value |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`                                                                                                                  | Backend GitHub OAuth         | shell env, `backend/.env`, or Render secrets                                                                                                                                                                                                                |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_SMTP_STARTTLS`                | Backend mail sender          | Local/dev launchers and Docker Compose force Mailpit; demo/demo-local use Brevo credentials from environment values                                                                                                                                         |
| `SPRING_MAIL_FROM_ADDRESS`, `SPRING_MAIL_FROM_NAME`                                                                                                         | Backend mail sender identity | `application.yml` plus env overrides                                                                                                                                                                                                                        |
| `APP_AUTH_*`                                                                                                                                                | Backend auth settings        | `application.yml` plus env overrides                                                                                                                                                                                                                        |
| `BOOTSTRAP_*`                                                                                                                                               | Backend demo seed accounts   | `application.yml` and `application-demo.yml` plus demo env overrides                                                                                                                                                                                        |
| `APP_PUBLIC_BASE_URL`                                                                                                                                       | Backend generated links      | `application.yml` and profile overrides                                                                                                                                                                                                                     |
| `APP_SHORT_LINK_*`                                                                                                                                          | Backend cleanup and expiry   | `application.yml` and env overrides                                                                                                                                                                                                                         |
| `APP_CACHE_PROVIDER`, `APP_CACHE_*_TTL`                                                                                                                     | Backend cache mode and TTLs  | `application.yml` and profile overrides; cache names are Java constants because they are internal wiring between `@Cacheable` annotations and the cache manager                                                                                             |
| `APP_RATE_LIMIT_*`                                                                                                                                          | Backend rate limiting        | `application.yml` and env overrides                                                                                                                                                                                                                         |
| `APP_SECURITY_PUBLIC_OBSERVABILITY`                                                                                                                         | Backend actuator protection  | `application.yml` defaults to `false`; `application-local.yml` and `application-dev.yml` default to `true` so local Prometheus can scrape metrics                                                                                                           |
| `APP_AI_ENABLED`, `APP_AI_PROVIDER`, `APP_AI_PROMPT_VERSION`, `APP_AI_OLLAMA_*`                                                                             | Backend AI enrichment        | `application.yml` defaults to enabled stub enrichment; `local` and `dev` profiles default to Ollama; local/dev launchers start the Ollama container and pull the configured model; demo/demo-local should stay on `stub`                                    |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS`                                                                                                                          | Backend CORS allowlist       | `application.yml` and `application-demo.yml`; comma-separated list, for example `http://localhost:5173,http://192.168.1.128:5173`                                                                                                                           |
| `VITE_API_BASE_URL`                                                                                                                                         | Frontend API client          | `frontend/.env` for local runs, Netlify build env for demo                                                                                                                                                                                                  |
| `VITE_DEV_SERVER_PORT`                                                                                                                                      | Frontend Vite dev server     | `frontend/.env` or the default from `frontend/vite.config.js`                                                                                                                                                                                               |
| `VITE_CLOUDFLARE_WEB_ANALYTICS_TOKEN`                                                                                                                       | Frontend demo analytics      | Leave blank locally; set in the GitHub `demo` environment or Netlify build env to inject the Cloudflare Web Analytics beacon                                                                                                                                |
| `RENDER_DEPLOY_HOOK_URL`, `RENDER_API_KEY`, `RENDER_BACKEND_SERVICE_ID`, `RENDER_HEALTH_URL`, `FRONTEND_SMOKE_URL`, `NETLIFY_AUTH_TOKEN`, `NETLIFY_SITE_ID` | Deploy / smoke helpers       | `infra/.env`                                                                                                                                                                                                                                                |
| `SONAR_TOKEN`, `SONAR_HOST_URL`                                                                                                                             | Local Sonar helper           | `infra/sonar/.env`                                                                                                                                                                                                                                          |

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

### AI Enrichment Defaults

AI link enrichment is intentionally safe by default, but local developer workflows use Ollama automatically:

- `APP_AI_ENABLED=true`
- `APP_AI_PROVIDER=stub` in the base and demo profiles
- `APP_AI_PROVIDER=ollama` in `local` and `dev`
- `APP_AI_PROMPT_VERSION=link-metadata-v1`
- `APP_AI_MAX_ATTEMPTS=2`
- `APP_AI_OLLAMA_BASE_URL=http://localhost:11434`
- `APP_AI_OLLAMA_MODEL=llama3.2:1b`
- `APP_AI_OLLAMA_TIMEOUT=60s`
- `APP_AI_OPENAI_BASE_URL=https://api.openai.com/v1`
- `APP_AI_OPENAI_API_KEY=` blank unless `APP_AI_PROVIDER=openai`
- `APP_AI_OPENAI_MODEL=gpt-4o-mini`
- `APP_AI_OPENAI_TIMEOUT=30s`

With the stub provider, the backend does not call OpenAI or any external LLM.
It derives deterministic demo metadata from the URL after `LinkCreatedEvent` is published.
With the Ollama provider, the backend calls the local Ollama HTTP API and asks for JSON metadata.
With the OpenAI-compatible provider, set `APP_AI_PROVIDER=openai` and provide `APP_AI_OPENAI_API_KEY`.
The OpenAI-compatible provider uses a chat-completions style HTTP endpoint and can also point at compatible gateways by changing `APP_AI_OPENAI_BASE_URL`.
The metadata is stored in `ai_link_metadata` and can be read from:

- `GET /api/v1/ai/links/{code}/metadata`
- `GET /api/v1/urls`, where each link may include `aiMetadata` when enrichment is available

Use `APP_AI_ENABLED=false` if you want to keep the lifecycle visible but mark metadata as `DISABLED`.
Real providers must not receive account secrets, JWTs, passwords, or private user profile data.
Only the target URL and link code should be sent for enrichment.

The local backend launcher starts Mailpit and Ollama, pulls the configured model, warms it with a tiny prompt, and runs the backend with `APP_AI_PROVIDER=ollama`:

```bash
bash ./scripts/dev/backend-local.sh
```

The Docker dev stack starts Postgres, Redis, Mailpit, Ollama, a one-shot model pull/warm-up service, backend, and frontend:

```bash
bash ./scripts/dev/fullstack-dev.sh
```

`demo-local` and live `demo` should keep `APP_AI_PROVIDER=stub` by default.
`demo-local` uses the `demo` profile to rehearse Render-like behavior, and live Render instances should not be expected to run a local LLM.
If you explicitly point demo-local at a reachable Ollama instance, enrichment can work, but it is not the recommended default.

### JWT Secret Resolution

The backend resolves the JWT secret in this order:

1. `APP_AUTH_JWT_SECRET` from the environment or `backend/.env`
2. `JWT_SECRET` from the environment or `backend/.env`
3. A built-in non-blank placeholder only in the `local` and `dev` profiles

That means:

- `local` and `dev` can start even if neither variable is set, although you should still put a real secret in `backend/.env` for normal use.
- `demo` and production-style runs still require a real secret and will fail fast if it is missing or blank.

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
wsl bash ./scripts/quality/backend-coverage.sh
```

```bash
bash ./scripts/quality/backend-style.sh
bash ./scripts/quality/backend-tests.sh
bash ./scripts/quality/backend-coverage.sh
```

Frontend checks:

```powershell
wsl bash ./scripts/quality/frontend-style.sh
wsl bash ./scripts/quality/frontend-tests.sh
wsl bash ./scripts/quality/frontend-coverage.sh
wsl bash ./scripts/dev/frontend-build.sh
```

```bash
bash ./scripts/quality/frontend-style.sh
bash ./scripts/quality/frontend-tests.sh
bash ./scripts/quality/frontend-coverage.sh
bash ./scripts/dev/frontend-build.sh
```

The full pre-push gate already runs backend coverage and frontend coverage. You can still run them manually when you want a focused coverage report:

```powershell
wsl bash ./scripts/quality/backend-coverage.sh
wsl bash ./scripts/quality/frontend-coverage.sh
```

Manual security checks:

```powershell
wsl bash ./scripts/security/check-dependencies.sh
```

```bash
bash ./scripts/quality/backend-coverage.sh
bash ./scripts/quality/frontend-coverage.sh
```

```bash
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
