# Backend Module Plan

## Purpose

The backend is a modular monolith. The goal is to keep business boundaries explicit while still shipping one deployable application.

The current shape is optimized for:

- clear ownership boundaries
- easy local testing
- extraction readiness later, if a module outgrows the monolith

## Current Modules

### 1. `shared`

Stable public surface shared across backend modules.

Responsibilities:

- `shared.api.*` request/response records grouped by API area
- `shared.events` event contracts that cross module boundaries
- `shared.ports` small interfaces that remove direct feature-module dependencies
- `shared.types` strict shared enums/value types
- `shared.seed` reusable demo seed data consumed by links, analytics, and AI

Rules:

- no infrastructure dependencies
- no business logic
- minimal surface area
- no feature-module entities, repositories, controllers, or services

### 2. `auth`

Identity, access, and account lifecycle behavior.

Responsibilities:

- login and registration
- JWT issuing and refresh sessions
- password reset and email verification
- GitHub OAuth
- user and role management
- admin user overview
- account profile and security actions

Core concepts:

- user account
- role
- refresh session
- account action token
- social identity

### 3. `links`

All short-link lifecycle behavior.

Responsibilities:

- create short links
- validate original URLs
- support custom aliases and random code generation
- handle expiration
- resolve redirect targets
- generate QR payloads
- maintain redirect-path caching
- publish click events
- implement shared link ownership/statistics ports

Core concepts:

- short link
- code generation
- redirect lookup
- QR generation
- ownership state

### 4. `analytics`

Click-event enrichment, persistence, and read models.

Responsibilities:

- consume click events
- enrich clicks with country/device/browser metadata
- store click history
- expose analytics summary endpoints
- maintain analytics cache invalidation

Potential enrichments:

- user-agent parsing
- device classification
- referrer capture
- geo enrichment

### 5. `ai`

AI enrichment for short-link metadata.

Responsibilities:

- consume link-created events
- generate title, summary, category, tags, icon, and suggested alias metadata
- support stub, Ollama, and OpenAI-compatible providers
- persist enrichment status and failures
- expose regeneration/read endpoints

Core concepts:

- AI metadata
- provider strategy
- prompt rendering
- async enrichment worker

### 6. `application`

Application composition and technical wiring.

Responsibilities:

- Spring Boot application bootstrap
- `platform.security` security configuration
- `platform.web` HTTP/CORS/request infrastructure
- `platform.admin` admin monitoring endpoints
- `platform.persistence` Flyway wiring
- `platform.cache` cache wiring
- `platform.observability` metrics wiring
- bootstrap data runners
- deployment-facing configuration

Rules:

- no domain logic that belongs in `auth`, `links`, `analytics`, or `ai`
- use it as the composition root and runtime shell
- keep technical application infrastructure under `io.weblinkpilot.platform.*`
- Spring Modulith uses `explicitly-annotated` detection, so only packages marked with `@ApplicationModule` become business modules

### 7. `build-support`

Build/reporting module only.

Responsibilities:

- aggregate JaCoCo reports across modules
- support coverage checks and Sonar integration

Rules:

- not a runtime module
- not part of the business domain

## Module Dependencies

- `auth`, `links`, `analytics`, and `ai` depend on `shared`
- `application` wires the runtime and depends on the feature modules
- `application` platform packages are infrastructure, not business modules
- `build-support` only aggregates reports
- feature modules should not depend on each other's internals
- cross-feature sync access should go through `shared.ports`
- cross-feature async communication should go through `shared.events`
- `build-support` does not participate in the runtime dependency graph

## Domain Boundaries

### URL Creation Boundary

Input:

- original URL
- optional alias
- optional expiration
- optional owner

Output:

- short code
- full short URL
- QR information

### Redirect Boundary

Input:

- short code
- request metadata

Output:

- redirect target
- redirect status
- click event publication

### Analytics Boundary

Input:

- click event

Output:

- persisted click
- aggregate updates
- dashboard-ready metrics

### Auth and Access Control Boundary

Input:

- login credentials
- registration data
- JWT token
- refresh token
- role checks

Output:

- authenticated principal
- role-aware access
- admin-only route authorization
- rotated access and refresh tokens

## Public API Shape

Public endpoints:

- `POST /api/v1/urls`
- `GET /r/{code}`
- `GET /q/{code}`
- `GET /api/v1/urls?page=0&size=10`
- `GET /api/v1/urls/{code}`
- `GET /api/v1/urls/{code}/qr`
- `GET /api/v1/urls/{code}/preview`
- `GET /api/v1/analytics/{code}`
- `GET /api/v1/analytics/{code}/count`
- `GET /api/v1/analytics/{code}/details`
- `GET /api/v1/ai/links/{code}/metadata`
- `POST /api/v1/ai/links/{code}/metadata/regenerate`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /api/v1/auth/account`
- `POST /api/v1/auth/account/password`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`
- `POST /api/v1/auth/email-verification/request`
- `POST /api/v1/auth/email-verification/confirm`
- `GET /api/v1/auth/oauth2/github/start`
- `GET /api/v1/auth/oauth2/github/callback`
- `POST /api/v1/auth/oauth2/github/complete`

Admin endpoints:

- `GET /api/v1/admin/overview`
- `GET /api/v1/admin/link-creators`
- `GET /api/v1/admin/users?page=0&size=10`
- `GET /api/v1/admin/monitoring`

## Data Model Snapshot

### `roles`

Fields:

- `id`
- `name`
- `description`

### `app_users`

Fields:

- `id`
- `username`
- `password_hash`
- `role_id`
- `enabled`
- `created_at`
- `last_login_at`

### `app_refresh_tokens`

Fields:

- `id`
- `token_hash`
- `user_id`
- `created_at`
- `expires_at`
- `revoked_at`

### `short_links`

Fields:

- `id`
- `code`
- `original_url`
- `custom_alias`
- `created_at`
- `expires_at`
- `status`
- `click_count`
- `owner_username`

### `click_events`

Fields:

- `id`
- `short_code`
- `clicked_at`
- `ip_address`
- `user_agent`
- `referrer`
- `country`
- `device_type`
- `browser_family`
- `event_source`

## Cache Strategy

Current cache strategy:

- hot short-link lookups use Redis
- analytics read paths use cacheable read models where it helps
- cache invalidation happens on new clicks
- refresh sessions are persisted in Postgres and mirrored in Redis under hashed token keys with TTL-based cache entries for fast lookup and rotation

Cache keys are versioned and implementation-specific.

## Bootstrapping Strategy

Local/dev:

- seed `admin` and `user` accounts
- seed a few starter links

Demo:

- seed accounts from environment variables
- keep the demo data minimal and explicit

## Extraction Readiness

Best future extraction candidates:

- `analytics`
- `links` if redirect volume or isolation needs grow
- `ai` if provider usage, queues, or cost controls become substantial

Keep extraction cheap by:

- keeping API DTOs, events, ports, and shared value types in `shared`
- avoiding direct cross-module persistence access
- using events for click propagation
- keeping the composition root in `application`
