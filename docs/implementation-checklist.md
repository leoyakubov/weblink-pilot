# Implementation Checklist

## Purpose

This checklist turns the planning documents into an execution order.

It helps us build the project in small, verifiable slices without jumping too early into deep implementation details.

## Current Status

| Status | Phase | Summary |
|---|---|---|
| Done | Phase 0 | Repo readiness, docs, scripts, and naming are in place. |
| Done | Phase 1 | Backend foundation is implemented. |
| Done | Phase 2 | URL lifecycle is implemented. |
| Done | Phase 3 | QR code support is implemented. |
| Done | Phase 4 | Analytics is implemented. |
| Done | Phase 5 | Frontend foundation is implemented. |
| Done | Phase 6 | Frontend feature set is implemented. |
| Done | Phase 7 baseline | Tests, observability, Docker, Sonar, ArchUnit, and Testcontainers are in place. |
| Done | Phase 7 follow-up | Deployment setup for Netlify and Render plus the live demo path. |
| Nice to do | Phase 8 | Environment profiles if and when they simplify the runtime story. |
| Done | Phase 9 | Redis-backed caching for URL hot lookups and analytics cache invalidation are implemented. |
| Next | Phase 10 | Monitoring admin page, Prometheus/Grafana stack, and optional JWT protection for monitoring endpoints. |
| Nice to do | Future evolution | Broker extraction only if async/event needs justify it. |

## Phase 0 - Repo readiness

Goals:

- final repo naming
- baseline docs in place
- clean `.gitignore`
- repo structure agreed

Exit criteria:

- planning docs committed
- README links updated
- tech stack finalized

## Phase 1 - Backend foundation

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

## Phase 2 - URL lifecycle

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

## Phase 3 - QR code support

Goals:

- generate QR code for a short URL
- expose QR endpoint
- make QR visible in the API contract and frontend

Exit criteria:

- backend can generate QR output
- frontend can display or download QR

## Phase 4 - Analytics

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

## Phase 5 - Frontend foundation

Goals:

- scaffold Vue app
- create mobile-first shell
- wire routing and base layout
- connect to backend API

Exit criteria:

- frontend loads and navigates
- create-link form is present
- API integration is working

## Phase 6 - Frontend feature set

Goals:

- create link flow UI
- success page with short link and QR
- analytics dashboard
- link details/history views

Exit criteria:

- core user journeys work on mobile and desktop
- UI is stable enough for demo and review

## Phase 7 - Production hardening

Goals:

- integration and end-to-end tests
- rate limiting
- logging and metrics
- Docker deployment
- documentation polish

Exit criteria:

- project runs in containers
- tests cover key flows
- README and docs describe how to use it

## Phase 8 - Environment profiles

Status: done for the local/demo split, with Maven convenience profiles for direct backend runs.

Goals:

- keep the local/demo runtime split explicit
- use Maven profiles only as a convenience for `spring-boot:run`
- document the local/dev/docker/test/demo configuration story

Exit criteria:

- environment-specific behavior is explicit
- configuration is easier to reason about
- local, container, and demo workflows stay simple

## Phase 9 - Redis caching

Status: done.

Goals:

- replace the in-memory cache with Redis for hot short-code lookups and cacheable analytics paths
- use Redis for rate-limiting counters if that becomes the simplest production path
- keep cache behavior explicit in local, demo, and deployment profiles

Exit criteria:

- URL lookups no longer rely only on the in-memory cache
- analytics and other hot paths can use Redis-backed caching where it helps
- cache behavior is documented and testable

## Phase 10 - Monitoring

Goals:

- add the internal monitoring page in the frontend
- link or embed health, metrics, Prometheus, and Grafana views
- add local Prometheus + Grafana support if needed for the deployed demo
- decide whether monitoring endpoints need JWT protection

Exit criteria:

- monitoring is visible from the app
- the deploy path stays simple
- the monitoring stack can be enabled after the app is live

## Suggested build order

1. backend foundation
2. URL lifecycle
3. QR support
4. analytics
5. frontend foundation
6. frontend features
7. hardening
8. deployment
9. Redis caching
10. monitoring
