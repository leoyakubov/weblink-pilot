# Roadmap

This document is the single planning source of truth.
The old implementation checklist has been merged here so we only maintain one plan.

## Status Overview

| # | Status | Area | Notes |
|---|---|---|---|
| 0 | &#x1F7E2; Done | [Project bootstrap](#phase-0-repo-readiness) | Repo layout, baseline docs, naming, and initial tooling are in place. |
| 1 | &#x1F7E2; Done | [Backend foundation](#phase-1-backend-foundation) | Modular backend, persistence, security, cache, actuator, logging, and observability are in place. |
| 2 | &#x1F7E2; Done | [Tests, code quality, Sonar](#phase-2-tests-code-quality-sonar) | Tests, coverage, ArchUnit, Testcontainers, and SonarQube coverage checks are in place. |
| 3 | &#x1F7E2; Done | [Docker](#phase-3-docker) | Local and dev Docker workflows are in place for the full stack and backend support services. |
| 4 | &#x1F7E2; Done | [URL lifecycle](#phase-4-url-lifecycle) | Create, read, redirect, custom alias, expiration, preview, QR, anonymous demo links, and signed-in owned links are implemented. |
| 5 | &#x1F7E2; Done | [QR code support](#phase-5-qr-code-support) | QR generation and the QR endpoint are in place for the core link journey. |
| 6 | &#x1F7E2; Done | [Analytics](#phase-6-analytics) | Click events are tracked by source (redirect and QR), with summaries and enrichment. |
| 7 | &#x1F7E2; Done | [Frontend foundation](#phase-7-frontend-foundation) | Vue app shell and backend integration are in place. |
| 8 | &#x1F7E2; Done | [Frontend feature set](#phase-8-frontend-feature-set) | Create flow, dashboard, history, details, QR UI, and sign in/sign up screens are in place. |
| 9 | &#x1F7E2; Done | [Authentication and access control](#phase-9-authentication-and-access-control) | JWT login/register/me flows, refresh cookies, password reset, email verification, user and admin roles, bootstrap seed data, role-aware navigation, and admin-only monitoring access are in place. |
| 10 | &#x1F7E2; Done | [Production hardening](#phase-10-production-hardening) | Logging, metrics, documentation polish, release checks, and deploy safety are in place. |
| 11 | &#x1F7E2; Done | [Environment profiles and scripts](#phase-11-environment-profiles-and-scripts) | Local, dev, and demo Spring profiles plus the helper scripts for direct runs and Docker workflows are in place. |
| 12 | &#x1F7E2; Done | [Redis cache](#phase-12-redis-caching) | Redis-backed hot short-code lookup caching and analytics cache invalidation are in place, and the broader cache map lives in [cache-redis-scenarios.md](../design/cache-redis-scenarios.md). |
| 13 | &#x1F7E2; Done | [Monitoring stack integration](#phase-13-monitoring) | The admin monitoring page links to backend health/info/metrics/prometheus, and the local Docker stack includes Prometheus and Grafana. |
| 14 | &#x1F7E2; Done | [Auth expansion](#phase-14-auth-expansion) | GitHub social login, richer account management, and remember-me session controls are in place on top of the current JWT, refresh-cookie, password-reset, and email-verification flow. |
| 15 | &#x1F7E2; Done | [Spring Modulith migration](#phase-15-spring-modulith-migration) | The backend module map is frozen, the public APIs are documented, and the Modulith-style verification tests are in place. |
| 16 | &#x1F7E2; Done | [Redis-first refresh tokens](#phase-16-redis-first-refresh-tokens) | Make refresh-token lookup and rotation Redis-first while keeping PostgreSQL as the durable source of truth. The refresh-token cache flow is documented in [cache-redis-scenarios.md](../design/cache-redis-scenarios.md). |
| 17 | &#x1F7E2; Done | [Async and non-blocking operations strategy](#phase-17-async-and-non-blocking-operations-strategy) | Document which flows should stay synchronous and which ones should move to async or deferred processing, starting with auth email delivery, analytics fan-out, cache updates, and reminder jobs. |
| 18 | &#x1F7E1; In Progress | [Frontend redesign](#phase-18-frontend-redesign) | Redesign the main frontend pages and forms with a Vue 3 admin/dashboard UI foundation, moving toward full Sakai adoption while preserving the current design as a rollback path. |
| 19 | Planned | [Demo visit analytics](#phase-19-demo-visit-analytics) | Integrate a ready-made analytics service for demo traffic so we can track visits, referrers, and basic usage without building our own analytics stack first. |
| 20 | Planned | [Security hardening follow-up](#phase-20-security-hardening-follow-up) | Address the remaining OWASP-style findings from the security review, especially CSRF strategy, XSS/token storage, observability exposure, CORS strictness, and abuse controls. |
| 21 | Planned | [Testing scenarios and README polish](#phase-21-testing-scenarios-and-readme-polish) | Add a README section with step-by-step test scenarios and flow diagrams, then reshape the main README into a more presentation-ready, product-style format. |
| 22 | Planned | [Codebase refactoring and security review](#phase-22-codebase-refactoring-and-security-review) | Review backend, frontend, and supporting files for best practices, coding smells, hardcoded values, magic strings and numbers, oversized classes, logging quality, sensitive data exposure, and overall security gaps. |
| 23 | Planned | [RabbitMQ async messaging](#phase-23-rabbitmq-async-messaging) | Add RabbitMQ if we want queued analytics, background jobs, or live event fan-out without pushing everything through the request thread. |
| 24 | Planned | [Expiry reminder emails](#phase-24-expiry-reminder-emails) | Add a scheduled backend job that scans each user's links, finds links nearing expiry, and emails a reminder list to the user. |

## Execution Checklist

Status note:

- Phases 0-14 are already shipped and documented here as the implemented baseline.
- Phases 15-24 are the remaining roadmap items, with phase 15 now complete, phase 16 complete, phase 17 complete, and phase 18 in progress.
- The auth baseline now includes refresh cookies, password reset, email verification, GitHub social login, and richer account management.
- Remaining phases include checklists with `[x]` for done items and `[ ]` for pending items.

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

Checklist:

- [x] Bootstrapped the repo layout
- [x] Agreed the final repo naming
- [x] Added the baseline docs
- [x] Cleaned the `.gitignore`
- [x] Confirmed the repo structure

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

Checklist:

- [x] Created the backend module structure
- [x] Wired the Spring Boot application
- [x] Configured PostgreSQL, Flyway, security, cache, and observability
- [x] Defined shared contracts and domain modules

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

Checklist:

- [x] Kept the test suite green
- [x] Maintained coverage checks
- [x] Enforced ArchUnit and integration test safety
- [x] Ran local SonarQube analysis

### Phase 3 - Docker

Goals:

- keep the dev Docker stack reproducible
- run backend, frontend, database, and cache through Compose
- mirror the demo deployment shape as closely as practical

Exit criteria:

- the full stack can be started locally
- the dev environment matches the demo topology closely
- container orchestration is documented and stable

Checklist:

- [x] Kept the dev Docker stack reproducible
- [x] Ran backend, frontend, database, and cache through Compose
- [x] Mirrored the demo deployment shape as closely as practical

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

Checklist:

- [x] Implemented short URL creation
- [x] Supported custom aliases
- [x] Supported expiration
- [x] Implemented redirect flow
- [x] Returned short link details

### Phase 5 - QR code support

Goals:

- generate QR code for a short URL
- expose QR endpoint
- make QR visible in the API contract and frontend

Exit criteria:

- backend can generate QR output
- frontend can display or download QR

Checklist:

- [x] Generated QR code for a short URL
- [x] Exposed the QR endpoint
- [x] Made QR visible in the API contract and frontend

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

Checklist:

- [x] Published click events
- [x] Consumed events asynchronously
- [x] Persisted click data
- [x] Exposed analytics summary endpoints
- [x] Distinguished redirect clicks from QR scans

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

Checklist:

- [x] Scaffolded the Vue app
- [x] Created the mobile-first shell
- [x] Wired routing and base layout
- [x] Connected the frontend to the backend API

### Phase 8 - Frontend feature set

Goals:

- create link flow UI
- success page with short link and QR
- analytics dashboard
- link details/history views

Exit criteria:

- core user journeys work on mobile and desktop
- UI is stable enough for demo and review

Checklist:

- [x] Built the create link flow UI
- [x] Added the success page with short link and QR
- [x] Added the analytics dashboard
- [x] Added link details/history views

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

Checklist:

- [x] Kept the JWT login/register/me flow
- [x] Kept refresh-cookie sessions, password reset, and email verification
- [x] Supported user and admin roles
- [x] Seeded bootstrap accounts and starter links
- [x] Made navigation and routes role-aware
- [x] Kept guest link creation available for demo users
- [x] Protected admin-only monitoring access

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

Checklist:

- [x] Added logging and metrics
- [x] Polished documentation
- [x] Added release checks and deploy safety

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

Checklist:

- [x] Kept the local/dev/demo runtime split explicit
- [x] Used Maven profiles only as a convenience for `spring-boot:run`
- [x] Documented the local/dev/docker/test/demo configuration story
- [x] Kept the helper scripts readable and predictable
- [x] Seeded local/dev users and demo bootstrap data in a profile-driven way

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
- the broader cache and Redis scenarios are documented in the design docs

Checklist:

- [x] Replaced the in-memory cache with Redis for hot short-code lookups
- [x] Used Redis for rate-limiting counters where useful
- [x] Kept cache behavior explicit in all profiles

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

Checklist:

- [x] Wired Prometheus/Grafana links into the admin monitoring page
- [x] Surfaced health and metrics in the UI
- [x] Evaluated JWT protection for monitoring endpoints

### Phase 14 - Auth expansion

Goals:

- GitHub social login is in place
- richer account management is in place
- remember-me session controls are in place

Exit criteria:

- account profile and password management are visible in the app
- linked social identities are visible in the account UI
- password change revokes existing refresh sessions
- the auth model supports the current product flow

Checklist:

- [x] Added GitHub social login
- [x] Added richer account management
- [x] Added remember-me session controls

### Phase 15 - Spring Modulith migration

Goals:

- freeze the current backend module boundaries as the canonical Modulith map
- define explicit public APIs for each module and keep internal packages private by convention
- keep Modulith-style structural verification tests alongside the existing ArchUnit checks
- replace direct cross-module calls with named interfaces or events where that improves coupling
- keep app-level bootstrap and scheduled maintenance limited to public module APIs
- document the final Maven module layout as a production-style modular monolith
- keep `auth` as its own backend module

Exit criteria:

- module boundaries are documented and enforced
- cross-module dependencies go through approved public interfaces
- the codebase has structural tests that fail when the module contract is broken
- the backend follows the frozen Spring Modulith arrangement

Checklist:

- [x] Freeze the final module map
- [x] Add Spring Modulith structural verification tests
- [x] Define named interfaces for approved public access
- [x] Replace direct cross-module calls with APIs or events
- [x] Verify the new module contracts in tests
- [x] Move bootstrap seeding and cleanup behind module APIs
- [x] Extract auth into a dedicated backend module
- [x] Freeze the final Maven module layout
- [x] Document the final public APIs for each module
- [x] Mark the Modulith migration as complete in the roadmap

### Phase 16 - Redis-first refresh tokens

Goals:

- make refresh-token lookup and rotation Redis-first
- keep PostgreSQL as the durable source of truth
- reduce refresh-token latency on login, refresh, and logout
- keep token revocation and rotation safe under concurrent requests
- document the cache and persistence split clearly, including the user-indexed auth cache flow

Exit criteria:

- refresh-token requests prefer Redis before falling back to PostgreSQL
- token rotation and revocation remain correct under concurrent access
- durable token state still survives cache loss
- the refresh-token flow is testable and documented

Checklist:

- [x] Design the Redis-first refresh-token lookup path
- [x] Keep PostgreSQL as the durable source of truth
- [x] Implement rotation and revocation with safe concurrency handling
- [x] Add tests for cache-hit, cache-miss, rotation, and logout paths
- [x] Document the refresh-token cache and persistence strategy

### Phase 17 - Async and non-blocking operations strategy

Goals:

- document which flows should stay synchronous and which should move to async or deferred processing
- start with the highest-value candidates such as auth email delivery, analytics fan-out, cache updates, and reminder jobs
- keep user-facing request paths short and predictable
- avoid introducing a reactive stack unless we have a clear payoff
- define where events, scheduled jobs, and background workers fit into the app

Exit criteria:

- the async vs sync decision rules are documented
- the main candidate flows are categorized
- the document is detailed enough to guide future implementation work
- the plan stays aligned with the current modular monolith architecture

Checklist:

- [x] Document the current async/non-blocking communication patterns
- [x] Classify the remaining product flows by sync vs async fit
- [x] Define implementation order for the best async candidates
- [x] Implement async auth email delivery with after-commit events
- [x] Implement async analytics cache invalidation with after-commit events
- [x] Document the retry and failure-handling expectations
- [x] Keep the plan aligned with the module communication map

### Phase 18 - Frontend redesign

Goals:

- redesign the main frontend pages and forms for a more polished demo presentation
- use PrimeVue as the component library and Sakai as the preferred free template/theme base
- keep the current design available as a rollback path while we move toward full Sakai adoption
- ship a visible legacy/Sakai switch so we can validate the new presentation safely
- keep the mobile-first experience strong while improving hierarchy, spacing, and visual clarity
- update the core screens so the demo feels more intentional and less like a scaffold
- preserve the existing app behavior while refreshing the look and feel
- remove wrapper and compatibility layers once the Sakai migration stabilizes
- unify the frontend `lib` and `shared/services` conventions
- extract repeated UI blocks from the larger pages into smaller components

Exit criteria:

- the main pages and forms look noticeably more polished
- the redesign remains mobile-first and responsive
- existing frontend flows continue to work
- the visual system is documented well enough to keep future pages consistent
- the fallback design path is preserved until the Sakai version is stable
- the frontend structure is cleaner and less duplicated than before

Checklist:

- [x] Pick PrimeVue as the frontend UI foundation
- [x] Pick Sakai as the free template/theme base
- [x] Redesign the main frontend pages
- [x] Redesign the core forms
- [x] Keep the mobile-first layout intact
- [x] Preserve existing frontend behavior
- [x] Document the updated visual direction
- [x] Remove the remaining wrapper and compatibility layers
- [ ] Unify `lib` vs `shared/services`
- [ ] Extract repeated UI blocks into smaller components

### Phase 19 - Demo visit analytics

Goals:

- integrate Cloudflare Web Analytics for the demo site
- track visits, referrers, and basic page usage for the public demo
- keep the analytics setup optional so local/dev can stay lightweight
- document the privacy and deployment implications of the chosen analytics provider

Exit criteria:

- demo traffic is visible in the Cloudflare analytics dashboard
- the snippet or integration is easy to enable and disable by environment
- local/dev remain usable without the external analytics service
- the analytics choice is documented alongside the deployment notes

Checklist:

- [x] Choose Cloudflare Web Analytics as the demo analytics provider
- [ ] Add the Cloudflare beacon snippet or integration
- [ ] Track visits, referrers, and basic page usage
- [ ] Keep local/dev optional and lightweight
- [ ] Document the privacy and deployment implications

### Phase 20 - Security hardening follow-up

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

Checklist:

- [ ] Review the remaining OWASP-style findings
- [ ] Tighten CSRF handling for cookie-based auth where needed
- [ ] Reduce browser-side token exposure risk
- [ ] Review public observability exposure, CORS, and abuse controls
- [ ] Close or explicitly accept the remaining findings

### Phase 21 - Testing scenarios and README polish

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

Checklist:

- [ ] Add step-by-step test scenarios for major app flows
- [ ] Add flow diagrams for auth, email, links, and analytics
- [ ] Rewrite the main README into a more presentation-ready form
- [ ] Keep the README aligned with the current implementation

### Phase 22 - Codebase refactoring and security review

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

Checklist:

- [ ] Review backend, frontend, and docs/scripts for best practices
- [ ] Replace hardcoded values and magic strings/numbers where appropriate
- [ ] Split large classes and services where it improves clarity
- [ ] Review logging for quality and sensitive-data exposure
- [ ] Run a full-project security review

### Phase 23 - RabbitMQ async messaging

Goals:

- add RabbitMQ only if the app needs queued work instead of synchronous request handling
- move click events or other background tasks off the request thread when that improves latency
- support event fan-out for future notifications, cache invalidation, or integration jobs
- keep local/dev/demo setup and tests explicit so the broker stays optional until the feature need is clear

Exit criteria:

- async event processing is documented and testable
- the broker is only used for workloads that benefit from queueing
- local and CI workflows still work cleanly when RabbitMQ is disabled

Checklist:

- [ ] Decide which workloads actually need queueing
- [ ] Define the event fan-out and background-job use cases
- [ ] Add broker configuration for local/dev/demo environments
- [ ] Add integration tests for the queue-backed flow
- [ ] Keep RabbitMQ optional until the need is proven

### Phase 24 - Expiry reminder emails

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

Checklist:

- [ ] Add the scheduled expiry scan job
- [ ] Build the list of links nearing expiry
- [ ] Send reminder mail to the owning user
- [ ] Deduplicate reminders across runs
- [ ] Document the mail workflow and test it

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
15. Spring Modulith migration
16. Redis-first refresh tokens
17. async and non-blocking operations strategy
18. frontend redesign
19. demo visit analytics
20. security hardening follow-up
21. testing scenarios and README polish
22. codebase refactoring and security review
23. RabbitMQ async messaging
24. expiry reminder emails
