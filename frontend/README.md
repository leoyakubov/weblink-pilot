# WebLinkPilot Frontend

Vue 3 mobile-first web app for creating short links, previewing redirect targets, showing QR codes, and browsing backend-powered link history plus analytics snapshots.

## Setup

```bash
cd frontend
npm install
```

Create a local env file if needed:

```bash
copy .env.example .env.local
```

If you run the backend from the repo root with the helper scripts, also put the shared repo-level secret in the repo root `.env.local`:

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
The About page in the app now holds the tech stack summary and the editable backend URL setting.

Guest users can create anonymous demo links immediately. If you sign in with the default demo credentials, new links become owned by your account and you can open the admin monitoring page if your account has the admin role.

Default demo credentials for the current backend:

- username: `admin`
- password: `admin123`
