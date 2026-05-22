# Backend Module Plan

## Purpose

The backend will be a modular monolith that behaves like a set of small services inside one deployable application.

The main goal is to keep domain boundaries explicit so that any high-value module can later be extracted into a microservice with minimal refactoring.

## Backend modules

### 1. `shared-contracts`

Responsibilities:

- shared request/response DTOs
- domain event contracts
- common error payloads
- pagination/filter contracts if needed later

Rules:

- no infrastructure dependencies
- no business logic
- stable API between modules

### 2. `url`

Responsibilities:

- create short URLs
- validate original URLs
- manage custom aliases
- handle expiration
- generate canonical short code
- return QR metadata or QR payload references

Core concepts:

- `ShortUrl`
- `ShortUrlCode`
- `Alias`
- `ExpirationPolicy`

### 3. `redirect`

Responsibilities:

- resolve short code to original URL
- serve redirect response
- keep redirect latency low
- emit click events asynchronously

Rules:

- no heavy analytics logic here
- minimal dependency surface
- cache-first read path

### 4. `analytics`

Responsibilities:

- consume click events
- enrich click data
- store event history
- expose reporting endpoints
- build aggregate read models

Potential enrichments:

- user-agent parsing
- device classification
- referrer capture
- IP-based geo approximation

### 5. `auth`

Responsibilities:

- protect management endpoints
- support user authentication later
- provide role-based access for dashboard actions

For v1:

- basic auth or simple auth boundary for admin-only routes

### 6. `infrastructure`

Responsibilities:

- database configuration
- cache configuration
- security wiring
- observability setup
- messaging adapter abstraction

Rules:

- should not contain domain rules
- should only wire technical concerns

## Module dependencies

Recommended dependency direction:

```text
shared-contracts
     ↑
url → redirect → analytics
     ↑
auth
     ↑
infrastructure
```

Better expressed as:

- domain modules depend on `shared-contracts`
- `infrastructure` depends on all modules for wiring
- modules should not depend on each other's internals
- cross-module interaction should happen through interfaces or events

## Domain boundaries

### URL creation boundary

Input:

- original URL
- optional alias
- optional expiration

Output:

- short code
- full short URL
- QR information

### Redirect boundary

Input:

- short code
- request metadata

Output:

- redirect target
- redirect status

### Analytics boundary

Input:

- click event

Output:

- persisted click
- aggregate update
- dashboard-ready metrics

## API plan

### Public API

- `POST /api/v1/urls`
- `GET /r/{code}`
- `GET /api/v1/urls/{code}`
- `GET /api/v1/urls/{code}/qr`
- `GET /api/v1/analytics/{code}`

### Management API

- `GET /api/v1/admin/urls`
- `GET /api/v1/admin/urls/{code}`
- `PATCH /api/v1/admin/urls/{code}`
- `DELETE /api/v1/admin/urls/{code}`

## Data model plan

### `short_urls`

Fields:

- `id`
- `code`
- `original_url`
- `custom_alias`
- `created_at`
- `expires_at`
- `status`
- `click_count`
- `created_by`

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

### Optional later tables

- `users`
- `api_keys`
- `link_tags`
- `link_groups`
- `audit_log`

## Event model

### `LinkCreated`

Emitted when a short link is successfully created.

Contains:

- code
- original URL
- alias flag
- expiration
- created timestamp

### `LinkClicked`

Emitted on each redirect.

Contains:

- code
- timestamp
- ip
- referrer
- user-agent

### `LinkDeleted` or `LinkDisabled`

Useful later for cleanup and analytics consistency.

## Cache strategy

Use cache-aside for redirect lookups.

Cache keys:

- `short-url:{code}`

Cached payload:

- original URL
- expiration
- status

Cache invalidation:

- on URL update
- on deletion
- on expiration handling if necessary

## QR code strategy

Backend owns QR generation because it is part of the product contract.

Possible outputs:

- PNG endpoint
- SVG endpoint
- Base64 payload in API response for quick preview

Recommended approach:

- canonical QR generation in backend
- frontend consumes preview or download link

## Error handling plan

Standard error categories:

- validation errors
- duplicate alias errors
- not found errors
- expired link errors
- auth errors
- rate limit errors

Error response should be stable and machine-readable.

## Testing plan

### Unit tests

- alias validation
- URL normalization
- code generation
- expiration rules
- event mapping

### Integration tests

- create and redirect flow
- cache fallback behavior
- analytics event handling
- auth-protected management endpoints

### Contract tests

- request and response schemas
- event payload compatibility

## Extraction readiness

The following modules are the best candidates for future extraction:

- `analytics`
- `redirect` if traffic or scaling demands it
- `auth` if identity becomes complex

The design should keep extraction cheap by:

- avoiding direct cross-module persistence access
- using events instead of hidden service calls
- keeping DTO contracts in `shared-contracts`

