# Roadmap

This document is the single planning source of truth.
The old implementation checklist has been merged here so we only maintain one plan.

## Status Overview

| #   | Status         | Area                                                                                               | Notes                                                                                                                                                                                                                                                                                                                                                |
| --- | -------------- | -------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0   | &#x1F7E2; Done | [Project bootstrap](#phase-0-repo-readiness)                                                       | Repo layout, baseline docs, naming, and initial tooling are in place.                                                                                                                                                                                                                                                                                |
| 1   | &#x1F7E2; Done | [Backend foundation](#phase-1-backend-foundation)                                                  | Modular backend, persistence, security, cache, actuator, logging, and observability are in place.                                                                                                                                                                                                                                                    |
| 2   | &#x1F7E2; Done | [Tests, code quality, Sonar](#phase-2-tests-code-quality-sonar)                                    | Tests, coverage, ArchUnit, Testcontainers, and SonarQube coverage checks are in place.                                                                                                                                                                                                                                                               |
| 3   | &#x1F7E2; Done | [Docker](#phase-3-docker)                                                                          | Local and dev Docker workflows are in place for the full stack and backend support services.                                                                                                                                                                                                                                                         |
| 4   | &#x1F7E2; Done | [URL lifecycle](#phase-4-url-lifecycle)                                                            | Create, read, redirect, custom alias, expiration, preview, QR, anonymous demo links, and signed-in owned links are implemented.                                                                                                                                                                                                                      |
| 5   | &#x1F7E2; Done | [QR code support](#phase-5-qr-code-support)                                                        | QR generation and the QR endpoint are in place for the core link journey.                                                                                                                                                                                                                                                                            |
| 6   | &#x1F7E2; Done | [Analytics](#phase-6-analytics)                                                                    | Click events are tracked by source (redirect and QR), with summaries and enrichment.                                                                                                                                                                                                                                                                 |
| 7   | &#x1F7E2; Done | [Frontend foundation](#phase-7-frontend-foundation)                                                | Vue app shell and backend integration are in place.                                                                                                                                                                                                                                                                                                  |
| 8   | &#x1F7E2; Done | [Frontend feature set](#phase-8-frontend-feature-set)                                              | Home/create, Links, Link details, Analytics overview/detail, QR UI, and sign in/sign up screens are in place.                                                                                                                                                                                                                                        |
| 9   | &#x1F7E2; Done | [Authentication and access control](#phase-9-authentication-and-access-control)                    | JWT login/register/me flows, refresh cookies, password reset, email verification, user and admin roles, bootstrap seed data, role-aware navigation, admin monitoring, and admin users are in place.                                                                                                                                                  |
| 10  | &#x1F7E2; Done | [Production hardening](#phase-10-production-hardening)                                             | Logging, metrics, documentation polish, release checks, and deploy safety are in place.                                                                                                                                                                                                                                                              |
| 11  | &#x1F7E2; Done | [Environment profiles and scripts](#phase-11-environment-profiles-and-scripts)                     | Local, dev, and demo Spring profiles plus the helper scripts for direct runs and Docker workflows are in place.                                                                                                                                                                                                                                      |
| 12  | &#x1F7E2; Done | [Redis cache](#phase-12-redis-caching)                                                             | Redis-backed hot short-code lookup caching and analytics cache invalidation are in place, and the broader cache map lives in [cache-redis-scenarios.md](../design/cache-redis-scenarios.md).                                                                                                                                                         |
| 13  | &#x1F7E2; Done | [Monitoring stack integration](#phase-13-monitoring)                                               | The admin monitoring page shows health checks, runtime metrics, configuration, service endpoints, and links to Prometheus/Grafana when the local stack is available.                                                                                                                                                                                 |
| 14  | &#x1F7E2; Done | [Auth expansion](#phase-14-auth-expansion)                                                         | GitHub social login, richer account management, and remember-me session controls are in place on top of the current JWT, refresh-cookie, password-reset, and email-verification flow.                                                                                                                                                                |
| 15  | &#x1F7E2; Done | [Spring Modulith migration](#phase-15-spring-modulith-migration)                                   | The backend module map is frozen, the public APIs are documented, and the Modulith-style verification tests are in place.                                                                                                                                                                                                                            |
| 16  | &#x1F7E2; Done | [Redis-first refresh tokens](#phase-16-redis-first-refresh-tokens)                                 | Make refresh-token lookup and rotation Redis-first while keeping PostgreSQL as the durable source of truth. The refresh-token cache flow is documented in [cache-redis-scenarios.md](../design/cache-redis-scenarios.md).                                                                                                                            |
| 17  | &#x1F7E2; Done | [Async and non-blocking operations strategy](#phase-17-async-and-non-blocking-operations-strategy) | Document which flows should stay synchronous and which ones should move to async or deferred processing, starting with auth email delivery, analytics fan-out, cache updates, and reminder jobs.                                                                                                                                                     |
| 18  | &#x1F7E2; Done | [Frontend redesign](#phase-18-frontend-redesign)                                                   | Redesign the main frontend pages and forms with a custom mobile-first SaaS UI, shared page/panel/link components, and PrimeVue controls where they are useful.                                                                                                                                                                                       |
| 19  | &#x1F7E2; Done | [Demo visit analytics](#phase-19-demo-visit-analytics)                                             | Added optional Cloudflare Web Analytics injection for the demo frontend, controlled by a build-time token so local/dev stay lightweight.                                                                                                                                                                                                             |
| 20  | &#x1F7E2; Done | [Security hardening follow-up](#phase-20-security-hardening-follow-up)                             | Added security headers, stricter CORS validation, deployment-safe metrics/prometheus access, and dedicated throttling for public auth endpoints; deeper BFF-style auth can remain a future product decision.                                                                                                                                         |
| 21  | &#x1F7E2; Done | [Testing scenarios and README polish](#phase-21-testing-scenarios-and-readme-polish)               | Added README test scenarios and flow diagrams for auth, email, link creation, redirects, analytics, admin monitoring, and account recovery.                                                                                                                                                                                                          |
| 22  | &#x1F7E2; Done | [AI link enrichment](#phase-22-ai-link-enrichment)                                                 | Added practical AI enrichment for created links with generated metadata, summary, category, tags, icon, suggested alias, provider abstraction, async processing, retry, and graceful fallback.                                                                                                                                                       |
| 23  | &#x1F7E2; Done | [Codebase refactoring and security review](#phase-23-codebase-refactoring-and-security-review)     | Completed backend maintainability/security pass: route policy constants, auth log masking, AI regenerate protection, typed link filters, link response mapper, demo seed catalog, token digest utility, monitoring split, refresh-token support split, AI provider helper split, URL creation split, and monitoring health status enum are in place. |
| 24  | &#x1F7E2; Done | [Pagination for list pages](#phase-24-pagination-for-links-and-analytics-list-pages)               | Links, analytics, home latest links, and admin users now use page-based backend contracts and shared frontend pagination controls.                                                                                                                                                                                                                    |
| 25  | &#x1F7E2; Done | [Email templating engine](#phase-25-email-templating-engine)                                       | Auth account emails now render through Thymeleaf text templates with a shared layout, focused renderer tests, and template workflow docs.                                                                                                                                                                                                             |
| 26  | Deferred       | [RabbitMQ async messaging](#phase-26-rabbitmq-async-messaging)                                     | Deferred until there is a proven need for broker-backed queueing beyond the current async/event approach.                                                                                                                                                                                                                                           |
| 27  | Deferred       | [Expiry reminder emails](#phase-27-expiry-reminder-emails)                                         | Deferred for now; link expiration exists, but scheduled reminder emails are not part of the current scope.                                                                                                                                                                                                                                           |

## Execution Checklist

Status note:

- Phases 0-25 are already shipped and documented here as the implemented baseline.
- Phases 26-27 are explicitly deferred and should not be picked up unless the product scope changes.
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
- define shared API/events/ports and domain modules

Exit criteria:

- backend starts locally
- database migrations run successfully
- health endpoint is available
- module boundaries are visible in code

Checklist:

- [x] Created the backend module structure
- [x] Wired the Spring Boot application
- [x] Configured PostgreSQL, Flyway, security, cache, and observability
- [x] Defined shared API/events/ports and domain modules

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
- analytics overview and per-link analytics detail views
- links list and link details views
- account, admin monitoring, and admin users routes

Exit criteria:

- core user journeys work on mobile and desktop
- main navigation matches the shipped route map
- UI is stable enough for demo and review

Checklist:

- [x] Built the create link flow UI
- [x] Added the success page with short link and QR
- [x] Added the analytics overview and per-link analytics detail views
- [x] Added Links and Link details views
- [x] Added Account, Admin monitoring, and Admin users pages

### Phase 9 - Authentication and access control

Goals:

- keep the shipped JWT login/register/me flow
- keep refresh-cookie sessions, password reset, and email verification working
- support user and admin roles
- seed bootstrap accounts and starter links in the auth layer
- make navigation and routes role-aware
- keep guest link creation available for demo users
- protect admin-only monitoring access
- protect the admin users directory

Exit criteria:

- users can sign up and sign in
- admins see admin-only navigation and routes
- admins can open monitoring and users from the account dropdown
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
- [x] Protected the admin users directory

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

- show backend health checks, runtime metrics, configuration, and service endpoints in the admin monitoring page
- wire Prometheus/Grafana links and local monitoring stack into the service endpoints panel
- decide whether any monitoring endpoints need additional JWT protection

Exit criteria:

- monitoring is visible from the app
- the deploy path stays simple
- the monitoring stack can be enabled after the app is live
- the admin monitoring page, health checks, configuration info, and local stack are consistent with the docs

Checklist:

- [x] Wired Prometheus/Grafana links into the admin monitoring page
- [x] Surfaced health checks, runtime metrics, configuration, and service endpoints in the UI
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
- keep the visual direction custom and product-specific instead of migrating to a Sakai template
- use PrimeVue only as the Vue control library for buttons, drawers, inputs, and password fields
- keep the mobile-first experience strong while improving hierarchy, spacing, and visual clarity
- update the core screens so the demo feels more intentional and less like a scaffold
- preserve the existing app behavior while refreshing the look and feel
- remove wrapper, template-mode, and compatibility layers that are no longer part of the chosen design direction
- unify the frontend `lib` and `shared/services` conventions
- extract repeated UI blocks from the larger pages into smaller components
- make page structure consistent: top navigation, page intro, then reusable panels
- keep operational frontend/backend settings on Monitoring rather than About

Exit criteria:

- the main pages and forms look noticeably more polished
- the redesign remains mobile-first and responsive
- existing frontend flows continue to work
- the visual system is documented well enough to keep future pages consistent
- the abandoned Sakai/template migration path is removed from active docs and CSS
- the frontend structure is cleaner and less duplicated than before

Checklist:

- [x] Keep PrimeVue as the control library already used by the app
- [x] Cancel the Sakai/template migration path
- [x] Redesign the main frontend pages
- [x] Redesign the core forms
- [x] Keep the mobile-first layout intact
- [x] Preserve existing frontend behavior
- [x] Document the updated visual direction
- [x] Remove the remaining wrapper and compatibility layers
- [x] Unify `lib` vs `shared/services`
- [x] Extract repeated UI blocks into smaller components
- [x] Extract page intro, panel, feature card, refresh button, and link list components
- [x] Move backend/browser settings from About to Monitoring
- [x] Merge Profile and Security into one compact Account settings page
- [x] Make QR actions use modal behavior consistently from history and home
- [x] Remove stale Sakai CSS mode and migration documentation

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
- [x] Add the Cloudflare beacon snippet or integration
- [x] Track visits, referrers, and basic page usage
- [x] Keep local/dev optional and lightweight
- [x] Document the privacy and deployment implications

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

- [x] Review the remaining OWASP-style findings
- [x] Keep refresh-cookie CSRF risk bounded with `SameSite=Lax` by default and document the remaining cross-site-cookie tradeoff
- [x] Reduce browser-side token exposure risk with security headers and CSP
- [x] Restrict metrics/prometheus by default, keep explicit local/dev scraping support, reject wildcard CORS origins with credentials, and add dedicated auth endpoint throttling
- [x] Close or explicitly accept the remaining findings in the security review

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

- [x] Add step-by-step test scenarios for major app flows
- [x] Add flow diagrams for auth, email, links, and analytics
- [x] Rewrite the main README into a more presentation-ready form
- [x] Keep the README aligned with the current implementation

### Phase 22 - AI link enrichment

Goals:

- add practical AI features that enrich short links without turning the product into a generic chatbot
- generate useful link metadata after creation: title, summary, category, tags, icon, and suggested alias
- keep link creation fast by processing AI enrichment asynchronously after the short link is created
- make the AI integration provider-agnostic so OpenAI, Azure OpenAI, Ollama, or a stub provider can be swapped by configuration
- keep local/dev safe and lightweight with deterministic stub responses by default, or disabled by configuration

Exit criteria:

- creating a link publishes an enrichment job or event without blocking the create-link response
- metadata can move through clear states such as `PENDING`, `READY`, `FAILED`, or `DISABLED`
- AI metadata is stored separately from core short-link lifecycle data
- link list APIs and link details can expose enrichment data when it is ready and a graceful pending/disabled state when it is not
- provider configuration, privacy implications, retry/error behavior, and local/demo setup are documented

Checklist:

- [x] Add a backend `ai` module with provider, prompt, service, repository, and event/listener boundaries
- [x] Define `AiProvider` plus `StubAiProvider` for local/tests
- [x] Add a real local provider implementation through Ollama for optional local/dev use
- [x] Add a hosted provider implementation for optional live demo use
- [x] Add persistence for AI link metadata with status, provider, prompt version, generated fields, and error details
- [x] Publish or handle link-created enrichment asynchronously after successful link creation
- [x] Generate title, summary, category, tags, icon, and suggested alias for created links
- [x] Expose metadata through a dedicated metadata endpoint
- [x] Display AI metadata on link details with pending, failed, and disabled states
- [x] Add aggregated metadata to link lists without N+1 frontend requests
- [x] Add manual metadata regeneration from the link details page
- [x] Document privacy rules: original URLs may be sent to the configured AI provider, but user secrets and account data must not be sent
- [x] Add tests for real provider parsing, retries, and API authorization boundaries

### Phase 23 - Codebase refactoring and security review

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

- [x] Review backend, frontend, and docs/scripts for best practices
- [x] Replace hardcoded values and magic strings/numbers where appropriate
- [x] Split large classes and services where it improves clarity
- [x] Review logging for quality and sensitive-data exposure
- [x] Run a full-project security review

Progress notes:

- Started with backend route-policy duplication: public auth route constants are now shared by Spring Security authorization and rate limiting.
- Started the sensitive logging pass: auth reset, verification, mail, controller, and GitHub-link logs now mask email addresses through one helper.
- Protected AI metadata regeneration: metadata reads remain public, but regeneration now requires authentication so anonymous traffic cannot trigger backend/provider work.
- Replaced raw link-list filter strings with `LinkSearchCriteria` and `ExpirationFilter`, and centralized link DTO creation in `LinkResponseMapper`.
- Extracted demo link and AI seed identity into `DemoSeedDataCatalog`, so link seeding, AI seed metadata, and monitoring checks share one source of truth.
- Extracted refresh-token SHA-256/base64-url hashing into `TokenDigest` with a focused unit test.
- Split admin monitoring into focused runtime metrics, health-check, configuration snapshot, and facade services.
- Split refresh-token Redis session caching, per-user token indexing, and after-commit execution into focused collaborators.
- Split AI provider prompt rendering and metadata JSON parsing into shared provider helpers.
- Split URL creation validation and generated-code allocation into focused collaborators.
- Replaced internal monitoring health-status strings with `AdminHealthStatus` while keeping API response values unchanged.
- Added [backend-code-quality-review.md](../reference/backend-code-quality-review.md) to track the Effective Java/code-smell review and remaining refactor candidates.
- Verified the completed pass with `./scripts/run-before-push.sh`: backend style, tests, coverage, SpotBugs, secret scan, frontend style, unit/component tests, frontend coverage, e2e, and production build all pass.
- Current accepted risk remains the browser-side access-token storage tradeoff documented in [security-review.md](../reference/security-review.md).

### Phase 24 - Pagination for links and analytics list pages

Goals:

- add page-based pagination to the links list and analytics list endpoints
- add page-based pagination to the admin users list endpoint
- expose pagination controls in the frontend tables and summary views
- keep sorting, filtering, and empty states consistent across all list surfaces
- update docs so the API contract and UI behavior stay discoverable

Exit criteria:

- large link and analytics lists remain usable without loading everything at once
- large admin users lists remain usable without loading everything at once
- backend endpoints return stable pagination metadata
- frontend pages support moving between pages without losing the current filters
- API and testing docs describe the pagination behavior

Checklist:

- [x] Add backend pagination to links list endpoints
- [x] Use the paginated links endpoint as the backend source for analytics overview pagination
- [x] Add backend pagination to admin users list endpoints
- [x] Add frontend pagination controls for links pages
- [x] Add frontend pagination controls for analytics pages
- [x] Add frontend pagination controls for admin users pages
- [x] Update API contract docs for the links list behavior
- [ ] Update testing docs and remaining references for the new list behavior

### Phase 25 - Email templating engine

Goals:

- move auth account emails away from ad hoc string assembly
- use a real templating engine for email layout and reusable fragments
- keep the email content easier to maintain, review, and localize
- preserve the current delivery flow and tests while changing only the rendering layer

Exit criteria:

- email templates are rendered through a proper templating engine
- auth emails share reusable layout/components
- email rendering has focused tests for the important variants
- docs explain how to update or add templates

Checklist:

- [x] Pick and wire the templating engine
- [x] Migrate password-reset and verification emails
- [x] Add template-focused tests
- [x] Document the email template workflow

### Phase 26 - RabbitMQ async messaging

Status: Deferred. Do not start this phase unless the app has a concrete queue-backed workload that the current synchronous/event-driven approach cannot handle cleanly.

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

### Phase 27 - Expiry reminder emails

Status: Deferred. Link expiration stays in scope, but scheduled reminder emails are intentionally out of the current implementation plan.

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
22. AI link enrichment
23. codebase refactoring and security review
24. pagination for links and analytics list pages
25. email templating engine
26. RabbitMQ async messaging - deferred
27. expiry reminder emails - deferred
