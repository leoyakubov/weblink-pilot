# WebLinkPilot Architecture Plan

## 1. Goal

Build a production-shaped URL shortening platform that starts as a modular monolith and is intentionally designed to be extractable into services later.

The system should support:

- short URL creation
- custom aliases
- expiration
- redirect handling
- QR code generation
- click analytics
- mobile-first web UI
- future event-driven scaling

## 2. Architectural Style

Primary style:

- modular monolith
- event-driven inside the monolith
- service boundaries enforced by code structure and module contracts

Why this is the right first step:

- fast to build and iterate
- easy to deploy as a single unit
- still demonstrates distributed-systems thinking
- enables later extraction of analytics or QR generation into separate services if needed

## 3. High-Level Modules

### Backend modules

- `url` - create short links, manage aliases, expiration, and code generation
- `redirect` - resolve short codes and serve redirects
- `analytics` - ingest click events and build reporting views
- `auth` - protect management APIs and dashboard actions
- `shared-contracts` - DTOs, events, and cross-module contracts
- `infrastructure` - persistence, cache, messaging adapter, security, observability

### Frontend module

- `web-ui` - Vue-based mobile-first interface for end users and link management

### Supporting modules

- `docs` - architecture notes, ADRs, and interview stories
- `infra` - Docker, local environment, and future deployment manifests

## 4. Request Flow

### Create short URL

1. User submits original URL, optional custom alias, optional expiration.
2. URL module validates input.
3. System generates or reserves the code.
4. Record is stored in PostgreSQL.
5. Cache is primed or invalidated.
6. Response returns short URL and QR code metadata.

### Redirect

1. User opens `https://domain/r/{code}` or scans QR code.
2. Redirect module checks cache first.
3. If cache misses, it falls back to PostgreSQL.
4. Redirect response is returned immediately.
5. Click event is published asynchronously for analytics.

### Analytics

1. Click event is captured as a domain event.
2. Analytics module consumes the event.
3. Enrichment is applied: IP, referrer, user-agent, device, geo.
4. Aggregated data is stored for reporting.

## 5. Event-Driven Strategy

We will use internal domain events first.

Recommended progression:

- phase 1: in-process domain events
- phase 2: transactional outbox
- phase 3: external broker if extraction becomes valuable

This gives us:

- clean decoupling
- a realistic migration path
- room to introduce Kafka or RabbitMQ later without rewriting the domain

## 6. Data Storage

### PostgreSQL

Stores:

- URL mappings
- click events
- aggregates for analytics views
- audit or ownership metadata if needed later

### Redis

Stores:

- hot short-code lookups
- rate limiting counters
- optional temporary QR or preview payloads

## 7. QR Code Strategy

QR code generation should be treated as a first-class feature.

Options:

- generate QR on the backend and return PNG/SVG
- also provide frontend preview and download actions

Recommended approach:

- backend owns canonical QR generation
- frontend consumes QR image or SVG endpoint

## 8. Future Extraction Path

If the system grows, the likely split is:

- `url-service`
- `redirect-service`
- `analytics-service`
- `notification-service` or future ops tooling

The key is to keep boundaries visible now so extraction later is a deployment change, not a redesign.

## 9. Non-Functional Requirements

- low-latency redirect path
- safe alias uniqueness
- cache-first hot path
- clear observability
- strong local dev experience
- easy deployment in Docker
- mobile-friendly frontend

