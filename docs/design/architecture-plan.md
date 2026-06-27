# WeblinkPilot Architecture Plan

## 1. Goal

Build a production-shaped URL shortener that is pleasant to use, easy to reason about, and realistic enough to show in interviews or demos.

The system currently supports:

- random short-code generation by default
- optional custom aliases
- expiration
- redirect handling
- QR code generation
- redirect-vs-QR analytics
- anonymous demo links and signed-in owned links
- JWT authentication with user and admin roles
- an admin-only monitoring page

## 2. Architectural Style

Primary style:

- modular monolith
- clear runtime composition in one deployable backend
- feature modules with explicit boundaries
- internal event and cache boundaries where they help latency or maintainability

Why this fits the project:

- fast to iterate on
- simple to deploy locally and in the demo environment
- still has room for later extraction if a module outgrows the monolith

## 3. Current Backend Modules

### `shared-contracts`

Shared DTOs and response contracts used across backend modules.

Responsibilities:

- request and response records
- shared response payloads
- admin overview contract

Rules:

- no infrastructure dependencies
- no business logic
- stable API surface between modules

### `url`

All short-link lifecycle behavior.

Responsibilities:

- create short links
- generate random codes
- support custom aliases
- handle expiration
- resolve redirects
- generate QR payloads
- publish click events
- keep the hot lookup path cache-friendly

### `analytics`

Click-event enrichment, persistence, and read models.

Responsibilities:

- consume click events
- enrich clicks with country/device/browser metadata
- store click history
- expose analytics summary endpoints
- manage analytics cache invalidation

### `app`

Runtime composition and technical wiring.

Responsibilities:

- Spring Boot application bootstrap
- security configuration
- JWT auth endpoints
- user and role management
- admin monitoring endpoints
- Flyway wiring
- cache wiring
- observability wiring
- bootstrap seed runners
- deployment-facing configuration

### `build-support`

Build and reporting module only.

Responsibilities:

- aggregate JaCoCo reports across modules
- support coverage checks and Sonar integration

## 4. Request Flow

### Create short URL

1. User submits an original URL, optionally with a custom alias.
2. The backend validates the request.
3. If no alias is provided, a random short code is generated.
4. The record is stored in PostgreSQL.
5. A starter response returns the short URL, QR URL, and created metadata.

### Redirect

1. User opens `https://domain/r/{code}` or scans a QR code.
2. The redirect path checks cache first.
3. If needed, it falls back to PostgreSQL.
4. A redirect response is returned immediately.
5. Click data is recorded and later used by analytics.

### Analytics

1. A click is recorded with source metadata.
2. Analytics enriches the click with country/device/browser data.
3. The event is persisted.
4. Aggregate read models are served to the dashboard.

### Auth and admin access

1. A user signs in or registers.
2. The backend issues or validates a JWT.
3. Role-aware navigation is shown in the frontend.
4. Admin-only routes remain restricted to `ADMIN`.

## 5. Data Storage

### PostgreSQL

Stores:

- users
- roles
- short links
- click events
- aggregate data needed by analytics

### Redis

Stores:

- hot short-link lookups
- cacheable analytics read paths
- cache invalidation targets after new clicks

### Local-only mode

For the `local` profile:

- H2 is used in-memory for the database
- cache stays in memory

## 6. Frontend Architecture

The frontend is a Vue 3 app with:

- a cleaner SaaS-style landing page
- separate sign-in and sign-up pages
- a public create flow that works for anonymous and signed-in users
- a dashboard for analytics inspection
- a details page for a single short link
- an admin-only monitoring page

## 7. Security Model

Public:

- redirect endpoints
- QR endpoints
- preview endpoints
- health/info endpoints
- sign-in and sign-up pages

Authenticated user:

- create owned links
- view own history and analytics

Admin:

- access the monitoring page
- access the admin overview endpoint

## 8. Environment Profiles

### `local`

- no Docker required
- H2 in-memory
- in-memory cache
- seeded users and starter links

### `dev`

- Docker Compose stack
- PostgreSQL
- Redis
- seeded users and starter links

### `demo`

- Render backend
- Netlify frontend
- Render PostgreSQL
- Render Redis
- environment-driven bootstrap data

## 9. QR Strategy

QR code generation is a first-class part of the product.

Recommended approach:

- backend owns canonical QR generation
- frontend consumes the QR endpoint for preview and copy/share flows

## 10. Cache Strategy

The cache is used for performance, not as a source of truth.

Current pattern:

- cache-aside for hot short-link lookups
- cacheable analytics reads where it helps
- explicit invalidation after new clicks

## 11. Monitoring Strategy

The app already has an admin monitoring page.

Current monitoring direction:

- keep the page inside the frontend
- surface health and metrics links there
- wire Prometheus/Grafana only as far as needed for the demo and local stack

## 12. Future Extraction Path

Most likely future extraction candidates:

- `analytics`
- `url` if redirect load or isolation needs grow

Auth is expected to stay in the `app` composition root unless it grows into a much larger subsystem.

## 13. Non-Functional Requirements

- low-latency redirect path
- safe alias uniqueness
- cache-friendly hot path
- clear observability
- strong local dev experience
- easy container and demo deployment
- mobile-friendly frontend
