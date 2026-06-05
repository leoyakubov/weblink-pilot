# Repository Structure Plan

## Recommended layout

```text
weblink-pilot/
|-- AGENTS.md
|-- backend/
|   |-- pom.xml
|   |-- shared-contracts/
|   |-- url/
|   |-- analytics/
|   `-- app/
|-- frontend/
|   |-- package.json
|   |-- src/
|   `-- tests/
|-- infra/
|   |-- docker-compose.yml
|   `-- deployment/
|-- scripts/
|   |-- win/
|   `-- unix/
`-- docs/
    |-- README.md
    |-- planning/
    |   |-- product-spec.md
    |   `-- roadmap.md
    |-- design/
    |   |-- architecture-plan.md
    |   |-- backend-module-plan.md
    |   |-- frontend-plan.md
    |   |-- adr.md
    |   |-- tech-stack.md
    |   `-- repo-structure.md
    |-- implementation/
    |   |-- api-contract-v1.md
    |   |-- development-standards.md
    |   `-- development-environment.md
    |-- testing/
    |   |-- feature-testing.md
    |   |-- auth-testing.md
    |   `-- backend-testing.md
    |-- operations/
    |   `-- deployment.md
    `-- reference/
        |-- interview-notes.md
        `-- security-review.md
```

## Why this layout

- keeps backend and frontend clearly separated
- allows one repo to host the whole product
- keeps deployment and docs alongside the code
- makes the docs easier to scan by SDLC phase
- keeps platform-specific scripts easy to find
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

## Workflow docs

- `AGENTS.md` is the root instruction file for coding agents.
- `docs/implementation/development-environment.md` is the local setup and verification guide.
- `docs/implementation/development-standards.md` is the quality and hygiene checklist.

## Decision

Use a monorepo first.

This keeps the project simpler to develop, test, and present in interviews.
