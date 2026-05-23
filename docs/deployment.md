# Deployment

## Chosen live-demo path

- frontend: Netlify
- backend: Oracle Cloud Always Free
- database: PostgreSQL in Docker on the Oracle VM

## Status

| Area | Status | Notes |
|---|---|---|
| Backend CI | Done | GitHub Actions runs Maven and frontend validation on push and PR. |
| Backend deploy workflow | Done | GitHub Actions builds and pushes the backend image and deploys it to Oracle. |
| Frontend deploy workflow | Done | GitHub Actions builds the Vue app and deploys it to Netlify. |
| Oracle runtime | Done | Backend, PostgreSQL, and Redis are defined in Docker Compose. |
| Frontend host config | Next | Set the Netlify site secrets and the backend API URL. |
| Backend public URL | Next | Use an HTTPS public URL for the Oracle VM or proxy the API through Netlify. |
| CORS origin | Next | Set the exact Netlify origin in the backend CORS allowlist env var. |
| Monitoring | Later | Add the monitoring admin page and stack after the app is live and stable. |

## GitHub Actions workflows

- CI: [`.github/workflows/ci.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/ci.yml)
- Oracle deploy: [`.github/workflows/deploy-backend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-backend.yml)
- Netlify deploy: [`.github/workflows/deploy-frontend.yml`](/C:/Users/dev/Desktop/weblink-pilot/.github/workflows/deploy-frontend.yml)

## Backend deploy secrets

- `ORACLE_HOST`
- `ORACLE_USER`
- `ORACLE_SSH_KEY`
- `ORACLE_POSTGRES_DB`
- `ORACLE_POSTGRES_USER`
- `ORACLE_POSTGRES_PASSWORD`
- `ORACLE_PUBLIC_BASE_URL`
- `ORACLE_CORS_ALLOWED_ORIGIN_PATTERNS`

## Frontend deploy secrets

- `NETLIFY_AUTH_TOKEN`
- `NETLIFY_SITE_ID`
- `VITE_API_BASE_URL`

## How it works

1. CI validates the backend and frontend on every push and pull request.
2. The backend workflow builds the Spring Boot image and publishes it to GHCR.
3. The backend workflow copies the Oracle Compose file and restarts the stack.
4. The frontend workflow builds the Vue app and publishes it to Netlify.

## Important note

The Netlify frontend will need a backend URL that is reachable from the browser or a same-origin proxy setup.
For a live demo, HTTPS is strongly recommended for the backend endpoint.

## Deployment checklist

Use this order so the secrets line up with the real runtime values.

### 1. Create the frontend on Netlify

Record:

- `NETLIFY_SITE_ID`
- the public site origin, for example `https://weblink-pilot.netlify.app`

Set:

- `VITE_API_BASE_URL` to the public backend API base, for example `https://api.weblink-pilot.example.com/api/v1`

### 2. Provision the Oracle VM

Install:

- Docker
- Docker Compose

Choose the backend public URL, for example:

- `https://api.weblink-pilot.example.com`

Record the SSH access details:

- `ORACLE_HOST`
- `ORACLE_USER`
- `ORACLE_SSH_KEY`

### 3. Decide the runtime secrets

Set these GitHub secrets:

- `ORACLE_POSTGRES_DB=weblinkpilot`
- `ORACLE_POSTGRES_USER=weblinkpilot`
- `ORACLE_POSTGRES_PASSWORD=<strong-password>`
- `ORACLE_PUBLIC_BASE_URL=https://api.weblink-pilot.example.com`
- `ORACLE_CORS_ALLOWED_ORIGIN_PATTERNS=https://weblink-pilot.netlify.app`
- `NETLIFY_AUTH_TOKEN=<netlify-personal-access-token>`
- `NETLIFY_SITE_ID=<netlify-site-id>`
- `VITE_API_BASE_URL=https://api.weblink-pilot.example.com/api/v1`

Notes:

- `ORACLE_CORS_ALLOWED_ORIGIN_PATTERNS` is a comma-separated list if you need more than one origin.
- The backend CORS allowlist should contain only the exact Netlify origin(s) you trust.

### 4. Deploy in this order

1. push to `main` or run the CI workflow
2. let the backend deploy workflow publish the Docker image and restart Oracle
3. let the frontend deploy workflow publish the Vue app to Netlify
4. open the live site and verify create-link, redirect, and analytics flows
