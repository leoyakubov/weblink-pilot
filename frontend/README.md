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

## Run

```bash
npm run dev
```

## Test

```bash
npm test
```

## Backend access

The frontend expects the backend to be available at `VITE_API_BASE_URL`.

Default dev credentials for the current backend:

- username: `admin`
- password: `admin123`
