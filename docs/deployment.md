# Deployment

## Chosen live-demo path

- frontend: Netlify
- backend: Render
- database: Render Postgres
- cache: Render Key Value (Redis)

## Status

| Area | Status | Notes |
|---|---|---|
| Backend CI | Done | GitHub Actions runs Maven and frontend validation on push and PR. |
| Backend deploy workflow | Done | GitHub Actions triggers the Render deploy hook after CI succeeds on `main`. |
| Frontend deploy workflow | Done | GitHub Actions builds the Vue app and deploys it to Netlify. |
| Deployment smoke tests | Done | GitHub Actions checks the live backend health endpoint and frontend home page after deploys. |
| Render runtime | Done | The backend runs on Render with Postgres and the demo profile. |
| Redis cache | Next | Add a Render Key Value instance and point the demo profile at its internal Redis URL. |
| Frontend host config | Done | Netlify site secrets and the backend API URL are configured. |
| Backend public URL | Done | The backend is reachable over HTTPS from the browser. |
| CORS origin | Done | The exact Netlify origin is allowed by the backend CORS config. |
| Monitoring | Later | Add the monitoring admin page and stack after the app is live and stable. |

## GitHub Actions workflows

- CI: [`.github/workflows/ci.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/ci.yml)
- Render deploy: [`.github/workflows/deploy-backend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-backend.yml)
- Netlify deploy: [`.github/workflows/deploy-frontend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-frontend.yml)
- Deployment smoke: [`.github/workflows/deployment-smoke.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deployment-smoke.yml)

## Backend deploy secret

- `RENDER_DEPLOY_HOOK_URL`

## Backend Render env vars

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

## Frontend deploy secrets

- `NETLIFY_AUTH_TOKEN`
- `NETLIFY_SITE_ID`
- `VITE_API_BASE_URL`
- `FRONTEND_SMOKE_URL`

## Optional keep-alive

If you use Render free and want to reduce cold starts, add a GitHub repository variable:

- `RENDER_HEALTH_URL=https://<your-render-backend>/actuator/health`
- `FRONTEND_SMOKE_URL=https://<your-netlify-site>/`

Then let the scheduled GitHub workflow ping that URL every 10 minutes.

If you store either URL in the `demo` environment instead, the deployment smoke and ping workflows will pick them up from that environment too.
For local manual smoke runs, the helper script also reads those values from the repo root `.env.local` automatically.

## How it works

1. CI validates the backend and frontend on every push and pull request.
2. The backend workflow triggers the Render deploy hook, which redeploys the Spring Boot service on Render.
3. Render runs the backend against Render Postgres with the `demo` profile.
4. The frontend workflow builds the Vue app and publishes it to Netlify.

## Important note

The Netlify frontend needs a backend URL that is reachable from the browser.
For a live demo, HTTPS is strongly recommended for the backend endpoint.
The deployed backend should run with `SPRING_PROFILES_ACTIVE=demo` so it picks up PostgreSQL, public URL, and CORS settings from `application-demo.yml`.

If you use Render's default service URL, `APP_PUBLIC_BASE_URL` can be omitted because the backend falls back to `RENDER_EXTERNAL_URL`.

## Deployment checklist

Use this order so the secrets line up with the real runtime values.

### 1. Create the frontend on Netlify

Record:

- `NETLIFY_SITE_ID`
- the public site origin, for example `https://weblink-pilot.netlify.app`

Set:

- `VITE_API_BASE_URL` to the public backend API base, for example `https://weblink-pilot.onrender.com/api/v1`

### 2. Create the backend on Render

Create:

- a Render Web Service for the backend
- a Render Postgres database
- a Render Key Value instance for Redis

Set in the Render dashboard:

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

### 3. Set the GitHub secrets

Add these repository secrets:

- `RENDER_DEPLOY_HOOK_URL`
- `NETLIFY_AUTH_TOKEN`
- `NETLIFY_SITE_ID`
- `VITE_API_BASE_URL`

Notes:

- `RENDER_DEPLOY_HOOK_URL` is optional if you rely entirely on Render's built-in Git auto-deploys, but we keep it for a GitHub-triggered deploy path.
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` should contain only the exact Netlify origin(s) you trust.
- `REDIS_URL` should use the Render Key Value instance's internal URL from the Connect menu.
- The bootstrap env vars seed the shared `admin / admin123` and `user / user123` accounts for demo/local/dev. Leave them empty if you want to opt out in a specific environment.
- The startup seeder also creates two anonymous starter links and one owned link for each seeded account so the first-run UI has real content.
- `FRONTEND_SMOKE_URL` should point to the live Netlify site root, for example `https://weblink-pilot.netlify.app/`.

### 4. Deploy in this order

1. push to `main` or run the CI workflow
2. let the backend deploy workflow trigger the Render redeploy
3. let the frontend deploy workflow publish the Vue app to Netlify
4. let the deployment smoke workflow verify the live backend and frontend
5. open the live site and verify create-link, redirect, analytics, and QR flows
