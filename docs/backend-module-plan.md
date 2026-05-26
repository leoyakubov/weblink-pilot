# Backend Module Plan

## Purpose

The backend is a modular monolith. The goal is to keep business boundaries explicit while still shipping one deployable application.

The current shape is optimized for:

- clear ownership boundaries
- easy local testing
- extraction readiness later, if a module outgrows the monolith

## Current Modules

### 1. `shared-contracts`

Shared DTOs and response contracts used across backend modules.

Responsibilities:

- request/response records
- shared response payloads
- admin overview contract
- any other cross-module API contract that must stay stable

Rules:

- no infrastructure dependencies
- no business logic
- minimal surface area

### 2. `url`

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

Core concepts:

- short link
- code generation
- redirect lookup
- QR generation
- ownership state

### 3. `analytics`

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

### 4. `app`

Application composition and technical wiring.

Responsibilities:

- Spring Boot application bootstrap
- security configuration
- JWT auth endpoints
- user and role management
- admin monitoring endpoints
- Flyway wiring
- cache wiring
- observability wiring
- bootstrap data runners
- deployment-facing configuration

Rules:

- no domain logic that belongs in `url` or `analytics`
- use it as the composition root and runtime shell

### 5. `coverage`

Build/reporting module only.

Responsibilities:

- aggregate JaCoCo reports across modules
- support coverage checks and Sonar integration

Rules:

- not a runtime module
- not part of the business domain

## Module Dependencies

- `url` and `analytics` depend on `shared-contracts`
- `app` wires the runtime and depends on the feature modules
- `coverage` only aggregates reports
- feature modules should not depend on each other's internals
- `coverage` does not participate in the runtime dependency graph

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
- `GET /api/v1/urls/{code}`
- `GET /api/v1/urls/{code}/qr`
- `GET /api/v1/urls/{code}/preview`
- `GET /api/v1/analytics/{code}`
- `GET /api/v1/analytics/{code}/count`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

Admin endpoints:

- `GET /api/v1/admin/overview`

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
- `url` if redirect volume or isolation needs grow

Keep extraction cheap by:

- keeping DTO contracts in `shared-contracts`
- avoiding direct cross-module persistence access
- using events for click propagation
- keeping the composition root in `app`
