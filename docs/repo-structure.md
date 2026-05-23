# Repository Structure Plan

## Recommended layout

```text
weblink-pilot/
├── backend/
│   ├── pom.xml
│   ├── shared-contracts/
│   ├── url/
│   ├── analytics/
│   └── app/
├── frontend/
│   ├── package.json
│   ├── src/
│   └── tests/
├── infra/
│   ├── docker-compose.yml
│   └── deployment/
└── docs/
    ├── architecture-plan.md
    ├── product-spec.md
    ├── roadmap.md
    └── repo-structure.md
```

## Why this layout

- keeps backend and frontend clearly separated
- allows one repo to host the whole product
- keeps deployment and docs alongside the code
- makes future extraction into separate repos possible if needed

## Backend structure

Backend should remain a modular monolith:

- `shared-contracts` for DTOs, events, and cross-module contracts
- `url` for URL lifecycle
- `analytics` for click event processing
- `app` for wiring, security, and runtime

## Frontend structure

Frontend should be a standalone Vue application:

- one app
- mobile-first responsive UI
- API-driven views
- QR code preview and analytics pages

## Decision

Use a monorepo first.

This keeps the project simpler to develop, test, and present in interviews.
