# Repository Structure Plan

## Recommended layout

```text
weblink-pilot/
|-- AGENTS.md
|-- backend/
|   |-- pom.xml
|   |-- shared/
|   |-- auth/
|   |-- links/
|   |-- analytics/
|   |-- ai/
|   |-- application/
|   `-- build-support/
|-- frontend/
|   |-- package.json
|   |-- public/
|   |-- src/
|   `-- tests/
|-- infra/
|   |-- docker-compose.yml
|   `-- sonar/
|-- scripts/
|   |-- dev/
|   |-- git/
|   |-- quality/
|   `-- security/
`-- docs/
    |-- README.md
    |-- planning/
    |   |-- product-spec.md
    |   `-- roadmap.md
    |-- design/
    |   |-- architecture-plan.md
    |   |-- backend-module-plan.md
    |   |-- module-communication.md
    |   |-- app-communication.md
    |   |-- cache-redis-scenarios.md
    |   |-- async-operations-strategy.md
    |   |-- frontend-plan.md
    |   |-- frontend-visual-system.md
    |   |-- spring-modulith-migration.md
    |   |-- adr.md
    |   |-- tech-stack.md
    |   `-- repo-structure.md
    |-- implementation/
    |   |-- api-contract-v1.md
    |   |-- email-templates.md
    |   |-- development-standards.md
    |   `-- development-environment.md
    |-- testing/
    |   |-- feature-testing.md
    |   |-- auth-testing.md
    |   `-- backend-testing.md
    |-- operations/
    |   `-- deployment.md
    `-- reference/
        |-- backend-code-quality-review.md
        |-- github-presentation.md
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

- `shared` for API DTOs, events, shared value types, ports, and reusable demo seed data
- `auth` for identity, roles, sessions, account actions, and OAuth
- `links` for short-link lifecycle
- `analytics` for click event processing
- `ai` for link metadata enrichment providers and workers
- `application` for the Spring Boot composition root plus `io.weblinkpilot.platform.*` runtime infrastructure
- `build-support` for coverage aggregation only

## Frontend structure

Frontend should be a standalone Vue application:

- `src/router` owns route registration and route groups
- `src/core` owns the app shell, navigation, and layout chrome
- `src/account` owns sign-in, registration, recovery, verification, OAuth completion, and account settings
- `src/admin` owns monitoring, browser reset, and users pages
- `src/features` owns product pages such as home, about, links, and analytics
- `src/shared` owns reusable UI components, composables, HTTP/settings services, types, and small utilities
- page folders keep `.vue`, `.ts`, `.css`, and tests side by side

## Workflow docs

- `AGENTS.md` is the root instruction file for coding agents.
- `docs/implementation/development-environment.md` is the local setup and verification guide.
- `docs/implementation/development-standards.md` is the quality and hygiene checklist.

## Decision

Use a monorepo first.

This keeps the project simpler to develop, test, and present in interviews.
