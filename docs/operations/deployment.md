# Deployment

## Chosen live-demo path

- frontend: Netlify
- backend: Render
- database: Render Postgres
- cache: Render Key Value (Redis)

## Architecture

The demo deployment is split into four layers:

1. GitHub Actions validates the code on push and pull request.
2. The backend deploy workflow triggers Render, waits for the new deploy to become live, then dispatches backend smoke.
3. The frontend deploy workflow builds the Vue app, publishes it to Netlify, captures the fresh deploy URL, then dispatches frontend smoke for that exact URL.
4. The smoke workflows run against live demo infrastructure, not against the skipped deploy path.

Important behavior:

- if a backend-only commit lands, the frontend deploy workflow skips and does not dispatch frontend smoke
- if a frontend-only commit lands, the backend deploy workflow skips and does not dispatch backend smoke
- if a deploy workflow skips because nothing relevant changed, no smoke job should start
- smoke uses the freshly deployed backend or frontend target, not the previous stable alias

### Flow diagram

```mermaid
flowchart TD
  A["GitHub push / PR"] --> B["CI"]
  B --> C["Deploy backend"]
  B --> D["Deploy frontend"]

  C --> C1["Trigger Render deploy"]
  C1 --> C2["Wait for live Render deploy"]
  C2 --> C3["Dispatch backend smoke"]
  C3 --> C4["Backend smoke checks live health endpoint"]

  D --> D1["Build Vue app"]
  D1 --> D2["Deploy to Netlify"]
  D2 --> D3["Capture fresh Netlify deploy URL"]
  D3 --> D4["Dispatch frontend smoke"]
  D4 --> D5["Frontend smoke checks fresh deployed URL"]

  C2 -. "skip if no backend-relevant changes" .-> S["No smoke"]
  D2 -. "skip if no frontend-relevant changes" .-> S
```

## Current state

| Area | Status | Notes |
|---|---|---|
| Backend CI | Done | GitHub Actions runs Maven and frontend validation on push and PR. |
| Backend deploy workflow | Done | GitHub Actions triggers the Render deploy hook after CI succeeds on `main`. |
| Backend readiness gate | Done | The backend deploy workflow waits for the new Render deploy to become live before it dispatches backend smoke. |
| Frontend deploy workflow | Done | GitHub Actions builds the Vue app and deploys it to Netlify. |
| Deployment smoke tests | Done | GitHub Actions checks the live backend health endpoint and the freshly deployed frontend URL after deploys with separate backend and frontend smoke workflows. Backend smoke is dispatched only after a real backend deploy finishes, and frontend smoke uses the Netlify deploy URL returned by the deploy step. |
| Render runtime | Done | The backend runs on Render with Postgres and the demo profile. |
| Redis cache | Next | Add a Render Key Value instance and point the demo profile at its internal Redis URL. |
| Frontend host config | Done | Netlify site secrets and the backend API URL are configured. |
| Backend public URL | Done | The backend is reachable over HTTPS from the browser. |
| CORS origin | Done | The exact Netlify origin is allowed by the backend CORS config. |
| Monitoring | Done | The admin page now links to backend health/info/metrics/prometheus, and Prometheus/Grafana are available in a separate optional monitoring stack. |

## GitHub Actions workflows

- CI: [`.github/workflows/ci.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/ci.yml)
- Render deploy: [`.github/workflows/deploy-backend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-backend.yml)
- Netlify deploy: [`.github/workflows/deploy-frontend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-frontend.yml)
- Deployment smoke backend: [`.github/workflows/smoke-backend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/smoke-backend.yml)
- Deployment smoke frontend: [`.github/workflows/smoke-frontend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/smoke-frontend.yml)

## Setup

### 1. GitHub repository setup

Use the repository `Settings` pages for secrets, variables, and the `demo` environment.

Create the `demo` environment first, then add secrets and variables there so the deploy and smoke workflows can read them.

Recommended GitHub values:

- repository secret `RENDER_DEPLOY_HOOK_URL`
- repository secret `RENDER_API_KEY`
- repository environment variable `RENDER_BACKEND_SERVICE_ID`
- repository secret `NETLIFY_AUTH_TOKEN`
- repository secret `NETLIFY_SITE_ID`
- repository secret `VITE_API_BASE_URL`
- repository environment variable `RENDER_HEALTH_URL`
- repository environment variable `FRONTEND_SMOKE_URL`

Use the `demo` environment for the values that should only exist in the live demo pipeline.
The workflows already read `vars.*` first and fall back to `secrets.*` where appropriate.

### 2. Netlify setup

Create or connect the frontend site in Netlify.

Set:

- build command: `npm run build`
- publish directory: `dist`
- site token: store as `NETLIFY_AUTH_TOKEN` in GitHub
- site ID: store as `NETLIFY_SITE_ID` in GitHub
- public backend URL: store as `VITE_API_BASE_URL` in GitHub, for example `https://weblink-pilot.onrender.com/api/v1`

The frontend workflow uses the Netlify CLI to deploy the built `dist` folder and reads the fresh deploy URL from the deploy output.
That URL is then passed to frontend smoke so the smoke job checks the exact fresh deploy.

### 3. Render setup

Create these Render resources:

- one Web Service for the backend
- one Postgres instance
- one Key Value instance for Redis

Set backend env vars in Render:

- `SPRING_PROFILES_ACTIVE=demo`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<render-postgres-host>:5432/<database>`
- `SPRING_DATASOURCE_USERNAME=<database-user>`
- `SPRING_DATASOURCE_PASSWORD=<database-password>`
- `REDIS_URL=<render-key-value-internal-url>`
- `BOOTSTRAP_ADMIN_USERNAME=<admin-username>`
- `BOOTSTRAP_ADMIN_PASSWORD=<admin-password>`
- `BOOTSTRAP_ADMIN_ROLE=ADMIN`
- `BOOTSTRAP_USER_USERNAME=<user-username>`
- `BOOTSTRAP_USER_PASSWORD=<user-password>`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://weblink-pilot.netlify.app`

Optional:

- `APP_PUBLIC_BASE_URL=https://weblink-pilot.onrender.com`

The backend deploy workflow uses:

- `RENDER_DEPLOY_HOOK_URL` to trigger a new Render deploy
- `RENDER_API_KEY` and `RENDER_BACKEND_SERVICE_ID` to verify the new deploy is actually live before backend smoke starts

## Smoke and keep-alive

If you use Render free and want to reduce cold starts, add GitHub repository variables:

- `RENDER_HEALTH_URL=https://<your-render-backend>/actuator/health`
- `FRONTEND_SMOKE_URL=https://<your-netlify-site>/`

Then let the scheduled GitHub workflow ping those URLs every 5 minutes.

If you store either URL in the `demo` environment instead, the deployment smoke and ping workflows will pick them up from that environment too.
For local manual smoke runs, the helper script also reads those values from `infra/.env.local` automatically.
The smoke output prints the backend HTTP status plus `status=UP`, and the frontend HTTP status plus the app shell marker (`id="app"`). By default, the helper script checks the local Docker stack; set `SMOKE_TARGET=demo` together with `RENDER_HEALTH_URL` and `FRONTEND_SMOKE_URL` to smoke the live demo instead. The PowerShell and Bash scripts also add spacing, color, and start/end banners so the backend and frontend checks are easy to scan separately.

## Important note

The Netlify frontend needs a backend URL that is reachable from the browser.
For a live demo, HTTPS is strongly recommended for the backend endpoint.
The deployed backend should run with `SPRING_PROFILES_ACTIVE=demo` so it picks up PostgreSQL, public URL, and CORS settings from `application-demo.yml`.

If you use Render's default service URL, `APP_PUBLIC_BASE_URL` can be omitted because the backend falls back to `RENDER_EXTERNAL_URL`.

## Runbook

1. push to `main` or run the CI workflow
2. let the backend deploy workflow trigger the Render redeploy
3. let the frontend deploy workflow publish the Vue app to Netlify
4. let the backend smoke workflow run only after the backend deploy workflow has confirmed a live Render deploy
5. let the frontend smoke workflow run only after the frontend deploy workflow has completed a real Netlify deploy and returned the deploy URL
6. open the live site and verify create-link, redirect, analytics, and QR flows
