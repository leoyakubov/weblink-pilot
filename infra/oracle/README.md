# Oracle Deployment

This folder contains the backend runtime definition for the Oracle Always Free VM.

## Services

- backend
- PostgreSQL
- Redis

## Required secrets

The GitHub Actions deployment workflow expects these secrets:

- `ORACLE_HOST`
- `ORACLE_USER`
- `ORACLE_SSH_KEY`
- `ORACLE_POSTGRES_DB`
- `ORACLE_POSTGRES_USER`
- `ORACLE_POSTGRES_PASSWORD`
- `ORACLE_PUBLIC_BASE_URL`
- `ORACLE_CORS_ALLOWED_ORIGIN_PATTERNS`

For the frontend workflow you will also need:

- `NETLIFY_AUTH_TOKEN`
- `NETLIFY_SITE_ID`
- `VITE_API_BASE_URL`

## Manual deploy steps

1. Build and push the backend Docker image from GitHub Actions.
2. Copy `infra/oracle/docker-compose.yml` to the Oracle host.
3. Write a `.env` file with the image tag and runtime values.
4. Run:

```bash
docker compose --env-file .env -f docker-compose.yml pull
docker compose --env-file .env -f docker-compose.yml up -d --remove-orphans
```

## Frontend note

The frontend is deployed separately on Netlify.
Set `VITE_API_BASE_URL` to the public backend URL, or proxy the API through Netlify if you prefer same-origin requests.
