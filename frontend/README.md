# WeblinkPilot Frontend

Vue 3 mobile-first web app for creating short links, previewing redirect targets, showing QR codes, and browsing backend-powered link history plus analytics snapshots.

## Routes

| Route              | Purpose                                                                                    |
| ------------------ | ------------------------------------------------------------------------------------------ |
| `/`                | Home page and create-link flow                                                             |
| `/links`           | Links list with filters and quick actions                                                  |
| `/link/:code`      | Link details, QR code, copy/share/open actions, and JSON preview                           |
| `/analytics`       | Analytics overview across visible links                                                    |
| `/analytics/:code` | Per-link analytics detail page                                                             |
| `/account`         | Account profile, password/security actions, and identity provider information              |
| `/about`           | Product, access, seeded data, stack, implementation, API endpoints, and project links      |
| `/monitoring`      | Admin monitoring with health checks, runtime metrics, configuration, and service endpoints |
| `/admin/users`     | Admin read-only users directory                                                            |
| `/settings/reset`  | Browser settings reset utility                                                             |

Auth and recovery routes live under `/auth/signin`, `/auth/signup`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/verify-email/request`, `/auth/verify-email`, and `/auth/github/complete`.

## Setup

```bash
cd frontend
npm install
```

Requires Node.js 24.16.0 LTS and npm 11.13.0.

Create a local env file if needed:

```bash
copy .env.example .env
```

The frontend uses `VITE_API_BASE_URL` for the backend API and `VITE_DEV_SERVER_PORT` for the local Vite port.

If you run the backend with the helper scripts, also put the shared backend secret in `backend/.env`:

```bash
JWT_SECRET=your-local-jwt-secret
```

## Run

```bash
npm run dev
```

Repo-level helper:

- local frontend shell: [`scripts/frontend/local-run-frontend.ps1`](../scripts/frontend/local-run-frontend.ps1)

Netlify needs a SPA fallback for direct route refreshes like `/dashboard`, so the repo includes `public/_redirects` with `/* /index.html 200`.

For demo performance, the repo also ships:

- lazy-loaded route pages for smaller initial bundles
- cache headers in `public/_headers` for hashed static assets
- gzip-enabled Nginx config for the container image
- a bundle inspection script: `npm run analyze:bundle`

## Test

```bash
npm test
```

For a one-shot run that does not stay in watch mode:

```bash
npm run test:run
```

For the Docker smoke check:

```bash
npm run smoke:docker
```

## Backend access

The frontend expects the backend to be available at `VITE_API_BASE_URL`.
The About page holds product and project reference information, while Monitoring owns the editable backend URL and browser reset utilities.

Guest users can create anonymous demo links immediately. If you sign in with the default demo credentials, new links become owned by your account, and admin accounts can open Monitoring and Users from the account dropdown.

Default demo credentials for the current backend:

- `admin / admin123`
- `user / user123`
