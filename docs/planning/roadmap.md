# Roadmap

This document is the single planning source of truth.
The old implementation checklist has been merged here so we only maintain one plan.

## Status Overview

| # | Status | Area | Notes |
|---|---|---|---|
| 0 | &#x1F7E2; Done | Project bootstrap | Repo layout, baseline docs, naming, and initial tooling are in place. |
| 1 | &#x1F7E2; Done | Backend foundation | Modular backend, persistence, security, cache, actuator, logging, and observability are in place. |
| 2 | &#x1F7E2; Done | Tests, code quality, Sonar | Tests, coverage, ArchUnit, Testcontainers, and SonarQube coverage checks are in place. |
| 3 | &#x1F7E2; Done | Docker | Local and dev Docker workflows are in place for the full stack and backend support services. |
| 4 | &#x1F7E2; Done | URL lifecycle | Create, read, redirect, custom alias, expiration, preview, QR, anonymous demo links, and signed-in owned links are implemented. |
| 5 | &#x1F7E2; Done | QR code support | QR generation and the QR endpoint are in place for the core link journey. |
| 6 | &#x1F7E2; Done | Analytics | Click events are tracked by source (redirect and QR), with summaries and enrichment. |
| 7 | &#x1F7E2; Done | Frontend foundation | Vue app shell and backend integration are in place. |
| 8 | &#x1F7E2; Done | Frontend feature set | Create flow, dashboard, history, details, QR UI, and sign in/sign up screens are in place. |
| 9 | &#x1F7E2; Done | Authentication and access control | JWT login/register/me flows, refresh cookies, password reset, email verification, user and admin roles, bootstrap seed data, role-aware navigation, and admin-only monitoring access are in place. |
| 10 | &#x1F7E2; Done | Production hardening | Logging, metrics, documentation polish, release checks, and deploy safety are in place. |
| 11 | &#x1F7E2; Done | Environment profiles and scripts | Local, dev, and demo Spring profiles plus the helper scripts for direct runs and Docker workflows are in place. |
| 12 | &#x1F7E2; Done | Redis cache | Redis-backed hot short-code lookup caching and analytics cache invalidation are in place. |
| 13 | &#x1F7E2; Done | Monitoring stack integration | The admin monitoring page links to backend health/info/metrics/prometheus, and the local Docker stack includes Prometheus and Grafana. |
| 14 | &#x1F7E1; In Progress | Auth expansion | Add GitHub social login, optional Google social login, and richer account management on top of the current JWT, refresh-cookie, password-reset, and email-verification flow. |
| 15 | Planned | RabbitMQ async messaging | Add RabbitMQ if we want queued analytics, background jobs, or live event fan-out without pushing everything through the request thread. |
| 16 | Planned | Expiry reminder emails | Add a scheduled backend job that scans each user's links, finds links nearing expiry, and emails a reminder list to the user. |

## Execution Checklist

Status note:

- Phases 0-13 are already shipped and documented here as the implemented baseline.
- Phases 14-16 are the remaining planned roadmap items.
- The auth baseline now includes refresh cookies, password reset, and email verification; only social login and richer account management remain in phase 15.

### Phase 0 - Repo readiness

Goals:

- project bootstrap
- final repo naming
- baseline docs in place
- clean `.gitignore`
- repo structure agreed

Exit criteria:

- planning docs committed
- README links updated
- tech stack finalized
- initial tooling agreed

### Phase 1 - Backend foundation

Goals:

- create backend module structure
- wire Spring Boot application
- configure PostgreSQL, Flyway, security, cache, and basic observability
- define shared contracts and domain modules

Exit criteria:

- backend starts locally
- database migrations run successfully
- health endpoint is available
- module boundaries are visible in code

### Phase 2 - Tests, code quality, Sonar

Goals:

- keep the test suite green
- maintain coverage checks
- enforce ArchUnit and integration test safety
- run local SonarQube analysis

Exit criteria:

- CI checks cover the important quality gates
- test and coverage reports are available
- the code quality feedback loop is quick

### Phase 3 - Docker

Goals:

- keep the dev Docker stack reproducible
- run backend, frontend, database, and cache through Compose
- mirror the demo deployment shape as closely as practical

Exit criteria:

- the full stack can be started locally
- the dev environment matches the demo topology closely
- container orchestration is documented and stable

### Phase 4 - URL lifecycle

Goals:

- implement short URL creation
- support custom aliases
- support expiration
- implement redirect flow
- return short link details

Exit criteria:

- user can create and resolve links
- redirect path is working end to end
- basic validation and error handling are in place

### Phase 5 - QR code support

Goals:

- generate QR code for a short URL
- expose QR endpoint
- make QR visible in the API contract and frontend

Exit criteria:

- backend can generate QR output
- frontend can display or download QR

### Phase 6 - Analytics

Goals:

- publish click events
- consume events asynchronously
- persist click data
- expose analytics summary endpoints
- distinguish redirect clicks from QR scans in the analytics model

Exit criteria:

- redirect path emits events
- QR scan path emits events separately
- analytics are stored and queryable
- metrics are visible for one short code

### Phase 7 - Frontend foundation

Goals:

- scaffold Vue app
- create mobile-first shell
- wire routing and base layout
- connect to backend API

Exit criteria:

- frontend loads and navigates
- create-link form is present
- API integration is working

### Phase 8 - Frontend feature set

Goals:

- create link flow UI
- success page with short link and QR
- analytics dashboard
- link details/history views

Exit criteria:

- core user journeys work on mobile and desktop
- UI is stable enough for demo and review

### Phase 9 - Authentication and access control

Goals:

- keep the shipped JWT login/register/me flow
- keep refresh-cookie sessions, password reset, and email verification working
- support user and admin roles
- seed bootstrap accounts and starter links in the auth layer
- make navigation and routes role-aware
- keep guest link creation available for demo users
- protect admin-only monitoring access

Exit criteria:

- users can sign up and sign in
- admins see admin-only navigation and routes
- anonymous and owned links both work
- the auth model supports the current product flow

### Phase 10 - Production hardening

Goals:

- logging and metrics
- documentation polish
- release checks and deploy safety

Exit criteria:

- project runs in containers
- tests cover key flows
- README and docs describe how to use it
- release and deployment docs match the shipped product

### Phase 11 - Environment profiles and scripts

Goals:

- keep the local/dev/demo runtime split explicit
- use Maven profiles only as a convenience for `spring-boot:run`
- document the local/dev/docker/test/demo configuration story
- keep the helper scripts readable and predictable
- seed local/dev users and demo bootstrap data in a profile-driven way

Exit criteria:

- environment-specific behavior is explicit
- configuration is easier to reason about
- local, container, and demo workflows stay simple
- helper scripts match the current folder layout and runtime behavior

### Phase 12 - Redis caching

Goals:

- replace the in-memory cache with Redis for hot short-code lookups and cacheable analytics paths
- use Redis for rate-limiting counters if that becomes the simplest production path
- keep cache behavior explicit in local, dev, demo, and deployment profiles

Exit criteria:

- URL lookups no longer rely only on the in-memory cache
- analytics and other hot paths can use Redis-backed caching where it helps
- cache behavior is documented and testable
- Redis cache/session behavior is documented in the implementation docs

### Phase 13 - Monitoring

Goals:

- wire Prometheus/Grafana links and local monitoring stack into the admin monitoring page
- surface health and metrics in the UI
- decide whether any monitoring endpoints need additional JWT protection

Exit criteria:

- monitoring is visible from the app
- the deploy path stays simple
- the monitoring stack can be enabled after the app is live
- the admin monitoring page and local stack are consistent with the docs

### Phase 14 - Auth expansion

Goals:

- add GitHub social login first
- add Google social login last, only if we want broader general-user sign-in
- support multiple auth providers if the product needs them
- add account management UI and API with dedicated tests and migrations

Exit criteria:

- users can sign in with a social provider if we choose to enable it
- the app can support more than one identity provider
- account lifecycle flows are explicit and testable
- admin and user account management remain separated cleanly

### Phase 15 - RabbitMQ async messaging

Goals:

- add RabbitMQ only if the app needs queued work instead of synchronous request handling
- move click events or other background tasks off the request thread when that improves latency
- support event fan-out for future notifications, cache invalidation, or integration jobs
- keep local/dev/demo setup and tests explicit so the broker stays optional until the feature need is clear

Exit criteria:

- async event processing is documented and testable
- the broker is only used for workloads that benefit from queueing
- local and CI workflows still work cleanly when RabbitMQ is disabled

### Phase 16 - Expiry reminder emails

Goals:

- add a scheduled backend job that scans each user's links
- build a list of links that are about to expire
- send reminder emails to the owning user
- keep the reminder logic compatible with the existing mail infrastructure
- make the job observable, testable, and safe to run repeatedly

Exit criteria:

- users receive a reminder before their links expire
- reminder emails are deduplicated or otherwise controlled
- the job is safe to run on a schedule without spamming users
- the notification flow is documented and testable

## Suggested Build Order

1. backend foundation
2. tests, code quality, Sonar
3. Docker
4. URL lifecycle
5. QR support
6. analytics
7. frontend foundation
8. frontend features
9. authentication and access control
10. production hardening
11. environment profiles and scripts
12. Redis caching
13. monitoring
14. auth expansion
15. RabbitMQ async messaging
16. expiry reminder emails

