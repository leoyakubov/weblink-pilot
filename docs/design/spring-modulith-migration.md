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

- business areas are split into `auth`, `url`, `analytics`, and `shared-contracts`
- the app has ArchUnit checks for cycles and layer boundaries
- module communication already uses application events in several places
- the runtime composition lives in `app`

What is still missing for a standard Spring Modulith setup:

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

Public surface candidates:

- `AuthService`
- `AccountManagementService`
- `RefreshTokenService`
- `EmailVerificationService`
- `PasswordResetService`
- `GitHubOAuthService`
- `OAuthLoginService`
- auth request and response contracts

### `url`

Responsible for:

- short-link creation
- alias handling
- expiration
- redirects
- QR generation
- redirect-path caching
- link events

Public surface candidates:

- `UrlService`
- `UrlLookupService`
- `RedirectService`
- `PublicUrlBuilder`
- `QrCodeService`
- `UrlCacheService`
- link request and response contracts

### `analytics`

Responsible for:

- click-event consumption
- enrichment
- persistence
- analytics summaries and counts

Public surface candidates:

- `AnalyticsQueryService`
- `ClickEventRecorder`
- `ClickEventConsumer`
- `UserAgentParser`
- analytics response contracts

### `shared-contracts`

Responsible for:

- shared request and response records
- event contracts that cross module boundaries

Rules:

- no infrastructure dependencies
- no business logic
- minimal and stable surface area

### `app`

Responsible for:

- Spring Boot bootstrap
- security configuration
- observability configuration
- rate limiting
- bootstrap data
- composition of the application

Rules:

- no core business logic that belongs in `auth`, `url`, or `analytics`
- treat this as the composition root

## Allowed Dependencies

### Recommended direction

- `auth` may depend on `shared-contracts`
- `url` may depend on `shared-contracts`
- `analytics` may depend on `shared-contracts`
- `app` may depend on the public APIs of the feature modules

### Dependency policy

- `auth` should not depend on `url` internals
- `url` should not depend on `analytics` internals
- `analytics` should not depend on hidden implementation classes from `url`
- internal repositories, entities, and helper classes should stay internal unless a module explicitly exposes them

## Current Pressure Points

These are the first places that should be revisited during migration:

### `analytics.web.AnalyticsController`

Current issue:

- it depends directly on `UrlLookupService`

Migration direction:

- expose a small public read API from `url`
- or define a named interface for the lookup contract

### `app.bootstrap.BootstrapLinkRunner`

Current issue:

- it talks directly to `ShortLinkRepository`

Migration direction:

- move seeding behavior behind a module-level API if the data belongs to the module behavior
- keep bootstrap logic in `app`, but reduce direct repository reach where possible

### `app.config.SecurityConfiguration`

Current issue:

- it depends on auth internals for security wiring

Migration direction:

- keep only public auth-facing contracts visible here
- avoid leaking implementation-only classes into the configuration layer

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

### Step 2 - Add structural checks

- add Spring Modulith structural verification tests
- keep the existing ArchUnit checks until the Modulith checks are stable
- fail fast when a module reaches into another module's internals

### Step 3 - Define named interfaces

- mark approved public packages or types as named interfaces
- keep the public surface narrow
- avoid exposing whole implementation packages just because one class is needed

### Step 4 - Replace direct cross-module calls

- use module APIs or events for module interaction
- keep direct service-to-service access only where the contract is intentionally public

### Step 5 - Review the composition root

- make sure `app` only wires the system together
- keep the feature logic inside feature modules

### Step 6 - Recheck tests and docs

- verify architecture tests
- verify module interaction tests
- update docs if a module boundary changed

## Definition Of Done

The migration is ready when:

- the module map is explicitly documented
- structural checks enforce the intended boundaries
- the public API of each module is clear
- direct cross-module coupling is reduced to the agreed minimum
- the codebase still behaves as one deployable backend

## Notes

- This project is already close to Modulith in spirit.
- The migration should be gradual, not a rewrite.
- We should keep the existing ArchUnit safety net until the new Modulith checks fully cover the same ground.
