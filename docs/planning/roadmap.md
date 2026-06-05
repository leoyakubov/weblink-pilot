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
| 14 | &#x1F7E2; Done | Auth expansion | GitHub social login and richer account management are in place on top of the current JWT, refresh-cookie, password-reset, and email-verification flow. Session duration controls such as remember-me remain a planned follow-up. |
| 15 | Planned | RabbitMQ async messaging | Add RabbitMQ if we want queued analytics, background jobs, or live event fan-out without pushing everything through the request thread. |
| 16 | Planned | Expiry reminder emails | Add a scheduled backend job that scans each user's links, finds links nearing expiry, and emails a reminder list to the user. |
| 17 | Planned | Demo visit analytics | Integrate a ready-made analytics service for demo traffic so we can track visits, referrers, and basic usage without building our own analytics stack first. |
| 18 | Planned | Security hardening follow-up | Address the remaining OWASP-style findings from the security review, especially CSRF strategy, XSS/token storage, observability exposure, CORS strictness, and abuse controls. |
| 19 | Planned | Remember-me session control | Add an explicit remember-me choice on sign in so users can opt into a longer refresh-cookie lifetime for trusted devices. |
| 20 | Planned | Testing scenarios and README polish | Add a README section with step-by-step test scenarios and flow diagrams, then reshape the main README into a more presentation-ready, product-style format. |
| 21 | Planned | Codebase refactoring and security review | Review backend, frontend, and supporting files for best practices, coding smells, hardcoded values, magic strings and numbers, oversized classes, logging quality, sensitive data exposure, and overall security gaps. |
| 22 | Planned | Spring Modulith migration | Audit the current backend modules, define explicit module boundaries and public interfaces, add Modulith-style verification tests, and gradually move cross-module interactions to named interfaces and events. |

## Execution Checklist

Status note:

- Phases 0-14 are already shipped and documented here as the implemented baseline.
- Phases 15-22 are the remaining planned roadmap items.
- The auth baseline now includes refresh cookies, password reset, email verification, GitHub social login, and richer account management.

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

- GitHub social login is in place
- richer account management is in place
- remember-me session controls are planned as a follow-up usability improvement

Exit criteria:

- account profile and password management are visible in the app
- linked social identities are visible in the account UI
- password change revokes existing refresh sessions
- the auth model supports the current product flow

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

### Phase 17 - Demo visit analytics

Goals:

- integrate a ready-made analytics service for the demo site
- track visits, referrers, and basic page usage for the public demo
- keep the analytics setup optional so local/dev can stay lightweight
- document the privacy and deployment implications of the chosen analytics provider

Exit criteria:

- demo traffic is visible in the selected analytics dashboard
- the snippet or integration is easy to enable and disable by environment
- local/dev remain usable without the external analytics service
- the analytics choice is documented alongside the deployment notes

### Phase 18 - Security hardening follow-up

Goals:

- review the remaining OWASP-style findings from the security review
- tighten CSRF handling for cookie-based auth where needed
- reduce browser-side token exposure risk and keep auth storage safe
- review public observability exposure, CORS patterns, and abuse controls
- keep the fixes testable and documented so future changes do not reintroduce the issues

Exit criteria:

- the known high-priority security gaps are tracked and closed or explicitly accepted
- the auth and browser token strategy is documented clearly
- observability, CORS, and abuse controls match the intended deployment model
- the security review stays aligned with the codebase

### Phase 19 - Remember-me session control

Goals:

- add an explicit remember-me option to the sign-in flow
- keep the default sign-in behavior short-lived and safe for shared devices
- extend the refresh-cookie lifetime only when the user opts in
- document how the access-token session and refresh-cookie duration differ with and without remember-me

Exit criteria:

- the sign-in form exposes a clear remember-me choice
- trusted-device sessions last longer without changing the access-token model
- logout and refresh-token revocation still work predictably
- the behavior is covered by auth testing docs and frontend test cases

### Phase 20 - Testing scenarios and README polish

Goals:

- add a README section that documents step-by-step test scenarios for key user journeys
- include simple flow diagrams for auth, email, link creation, and analytics verification
- rewrite the main README into a clearer presentation-ready format that looks like a product landing page plus developer guide
- keep the README structure aligned with the rest of the docs so testing, setup, and feature flows stay easy to scan

Exit criteria:

- the README includes concrete test scenarios with steps and expected results
- flow diagrams make the major app journeys easy to understand at a glance
- the main README feels polished and demo-friendly instead of purely technical
- the README remains consistent with the current app behavior and roadmap

### Phase 21 - Codebase refactoring and security review

Goals:

- review backend, frontend, and shared docs/scripts for best practices and code smells
- replace hardcoded values, magic strings, and magic numbers where a clear config or constant is better
- split large classes and services when they are doing too much
- review logging for useful structure, consistency, and accidental sensitive-data exposure
- run a full-project security review to catch common OWASP-style issues and risky implementation patterns

Exit criteria:

- the main code paths are easier to read and maintain
- hardcoded configuration and duplicated logic are reduced
- oversized classes are broken up where it improves clarity
- logs avoid sensitive data and are consistent with the deployment model
- the remaining security issues are documented, fixed, or explicitly accepted

### Phase 22 - Spring Modulith migration

Goals:

- audit the current backend module boundaries and decide the final Modulith module map
- define explicit public APIs for each module and keep internal packages private by convention
- add Modulith-style structural verification tests alongside the existing ArchUnit checks
- replace direct cross-module calls with named interfaces or events where that improves coupling
- decide whether the current Maven module layout should stay as-is or be flattened toward a more standard Modulith arrangement

Exit criteria:

- module boundaries are documented and enforced
- cross-module dependencies go through approved public interfaces
- the codebase has structural tests that fail when the module contract is broken
- the backend is on a clear path to a standard Spring Modulith arrangement

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
17. demo visit analytics
18. security hardening follow-up
19. remember-me session control
20. testing scenarios and README polish
21. codebase refactoring and security review
22. Spring Modulith migration

