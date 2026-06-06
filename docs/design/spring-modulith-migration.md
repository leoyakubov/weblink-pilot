# Spring Modulith Migration Plan

## Purpose

This document is the working plan for moving the backend toward a standard Spring Modulith style.

It captures:

- the current backend module map
- the intended public APIs for each module
- allowed dependency directions
- the first concrete code paths that should change during migration
- the order in which we should migrate without breaking the app

This is a migration guide, not an implementation record.

## Current State

The backend already behaves like a modular monolith:

- business areas are split into Maven modules: `auth`, `links`, `analytics`, and `shared-contracts`
- the runtime composition lives in `application`
- application-level bootstrap delegates demo-link seeding and cleanup work to module services
- configuration packages that are intentionally shared across modules are declared as named interfaces
- module communication already uses application events in several places
- ArchUnit and Spring Modulith structure checks are both present

## Final Module Map

This is the frozen module map for the current backend:

| Module | Purpose | Public surface |
|---|---|---|
| `application` | Spring Boot composition root, bootstrap, config, observability, rate limiting | No business API; wires the app together |
| `auth` | Login, registration, refresh sessions, password reset, email verification, GitHub OAuth, account management | `AuthService`, `AccountManagementService`, `RefreshTokenService`, `EmailVerificationService`, `PasswordResetService`, `GitHubOAuthService`, `OAuthLoginService`, `UserAccountService`, `RoleCatalogService`, `AdminOverviewService`, auth DTOs, `auth.config` |
| `links` | Short-link creation, alias handling, redirects, QR, caching, cleanup, seeding | `UrlService`, `UrlLookupService`, `RedirectService`, `PublicUrlBuilder`, `QrCodeService`, `UrlCacheService`, `UrlBootstrapService`, `ShortLinkCleanupService`, `UrlStatisticsService`, link DTOs, `links.config`, `links.service` |
| `analytics` | Click-event persistence, consumption, enrichment, summaries | `AnalyticsQueryService`, `ClickEventRecorder`, `ClickEventConsumer`, `UserAgentParser`, analytics DTOs |
| `shared-contracts` | Stable request/response/event contracts shared across modules | `contracts` |
| `build-support` | Technical build-only aggregation for coverage reporting | No runtime API; excluded from Modulith boundary thinking |

### Dependency Policy

- `application` may depend on the public APIs of the feature modules
- `application` may depend on named interfaces such as `auth.config`, `links.config`, and `links.service`
- `auth` may depend on `shared-contracts`
- `auth` may depend on the public `links` statistics facade for admin ownership counts
- `links` may depend on `shared-contracts`
- `analytics` may depend on `shared-contracts`
- internal repositories, entities, and helper classes stay internal unless a module explicitly exposes them

## What Was Still Missing

This list described the remaining work before the freeze and is now kept here for historical context:

- explicit application-module boundaries
- public API declarations per module
- named interfaces for approved cross-module access
- module-structure verification tests from Spring Modulith
- a documented dependency policy for each module

## Target Outcome

The target is a backend where:

- each feature module has a clearly defined public API
- internal classes stay internal by convention and by tests
- cross-module access goes through approved interfaces or events
- structural violations fail tests early
- the backend stays a single deployable application

The goal is not to introduce microservices.

## Proposed Module Map

### `auth`

Responsible for:

- login and registration
- JWT issuing and refresh sessions
- password reset and email verification
- account profile and password management
- social login flows
- admin account overview

### `links`

Responsible for:

- short-link creation
- alias handling
- expiration
- redirects
- QR generation
- redirect-path caching
- link events

### `analytics`

Responsible for:

- click-event consumption
- enrichment
- persistence
- analytics summaries and counts

### `shared-contracts`

Responsible for:

- shared request and response records
- event contracts that cross module boundaries

Rules:

- no infrastructure dependencies
- no business logic
- minimal and stable surface area
- expose contracts through the `contracts` named interface

### `application`

Responsible for:

- Spring Boot bootstrap
- security configuration
- observability configuration
- rate limiting
- bootstrap data
- composition of the application

Rules:

- no core business logic that belongs in `auth`, `links`, or `analytics`
- treat this as the composition root

## Allowed Dependencies

### Recommended direction

- `auth` may depend on `shared-contracts`
- `auth` may depend on the public URL statistics facade for admin ownership counts
- `links` may depend on `shared-contracts`
- `analytics` may depend on `shared-contracts`
- `application` may depend on the public APIs of the feature modules
- `application` may depend on named interfaces such as `auth.config` and `links.config` when it is acting as the composition root

### Dependency policy

- `auth` should not depend on `links` internals
- `links` should not depend on `analytics` internals
- `analytics` should not depend on hidden implementation classes from `links`
- internal repositories, entities, and helper classes should stay internal unless a module explicitly exposes them

## Current Pressure Points

These are the first places that should be revisited during migration:

### `analytics.web.AnalyticsController`

Current issue:

- it depends directly on `UrlLookupService`

Migration direction:

- expose a small public read API from `links`
- or define a named interface for the lookup contract

### `application.bootstrap.BootstrapLinkRunner`

Current issue:

- it talks directly to `ShortLinkRepository`

Migration direction:

- move seeding behavior behind a module-level API if the data belongs to the module behavior
- keep bootstrap logic in `application`, but reduce direct repository reach where possible

### `application.config.SecurityConfiguration`

Current issue:

- it depends on auth internals for security wiring

Migration direction:

- keep only public auth-facing contracts visible here
- avoid leaking implementation-only classes into the configuration layer
- treat `auth.config` and `links.config` as deliberate named interfaces, not accidental internals

### `auth.service.AdminOverviewService`

Current issue:

- it used to depend directly on `links.repository.ShortLinkRepository`

Migration direction:

- use a public URL-module statistics service instead of repository access
- keep the admin overview logic in `auth`
- expose only the minimal URL read model needed by admin summaries

### `auth.web.AuthController`

Current issue already fixed for the architecture test:

- frontend redirect URL building moved out of the controller

Migration direction:

- keep web controllers thin
- move non-HTTP logic into services

## Migration Steps

### Step 1 - Freeze the module map

- document the final module list
- decide which packages are public and which are internal
- keep this document aligned with the roadmap
- keep application-level bootstrap limited to orchestration over public module APIs

### Step 2 - Add structural checks

- add Spring Modulith structural verification tests
- keep the existing ArchUnit checks until the Modulith checks are stable
- fail fast when a module reaches into another module's internals

Current implementation progress:

- Spring Modulith verification now runs in the backend build
- `bootstrap` and `config` are intentionally outside the business-module scan because they belong to the composition root
- `auth` is now a dedicated Maven module instead of living inside `application`
- `links` and `analytics` use named interfaces for the approved public surfaces
- `auth` depends on the public `links` statistics facade instead of the repository layer
- demo link seeding now lives behind `links.service.UrlBootstrapService`
- cleanup job logic now lives behind `links.service.ShortLinkCleanupService`
- `auth.config`, `links.config`, and `links.service` are explicit named interfaces
- Maven modules now use production-style names: `application`, `auth`, `links`, `analytics`, `shared-contracts`

### Step 3 - Define named interfaces

- mark approved public packages or types as named interfaces
- keep the public surface narrow
- avoid exposing whole implementation packages just because one class is needed

### Step 4 - Replace direct cross-module calls

- use module APIs or events for module interaction
- keep direct service-to-service access only where the contract is intentionally public

### Step 5 - Review the composition root

- make sure `application` only wires the system together
- keep the feature logic inside feature modules

### Step 6 - Recheck tests and docs

- verify architecture tests
- verify module interaction tests
- update docs if a module boundary changed

## Definition Of Done

The migration is ready when:

- the module map is explicitly documented
- `auth`, `links`, `analytics`, `shared-contracts`, `build-support`, and `application` are represented as separate Maven modules
- structural checks enforce the intended boundaries
- the public API of each module is clear
- direct cross-module coupling is reduced to the agreed minimum
- the codebase still behaves as one deployable backend

## Freeze Note

The module map and public APIs in this document are now treated as the canonical reference for the current backend shape. Any later change to the module boundaries should update this document first, then the roadmap.

## Notes

- This project is already close to Modulith in spirit.
- The migration should be gradual, not a rewrite.
- We should keep the existing ArchUnit safety net until the new Modulith checks fully cover the same ground.
