# Application Communication Map

## Purpose

This document shows how the frontend and backend work together as one application.

It complements the module communication map by showing the user-facing flows end to end.

For the Redis/cache breakdown behind those flows, see [cache-redis-scenarios.md](cache-redis-scenarios.md).

## High-Level View

```mermaid
flowchart LR
  U["User"] --> F["Frontend (Vue 3)"]
  F -->|"sync: HTTP API"| B["Backend (Spring Boot modular monolith)"]
  B -->|"sync: repositories"| DB["PostgreSQL"]
  B -->|"sync: cache-aside"| R["Redis"]
  B -->|"async: domain events"| E["Analytics / background handlers"]
```

Legend:

- `sync` means the caller waits for the response.
- `async` means the backend publishes an event and continues without waiting for the consumer.

## Responsibilities By Layer

### Frontend

- renders the public app shell
- handles form input and validation
- calls backend HTTP APIs
- stores only non-sensitive UI state
- reacts to auth state and role-aware navigation

### Backend

- owns business rules
- validates and persists data
- issues auth tokens and cookies
- handles redirects and analytics events
- serves admin and monitoring endpoints

### Database And Cache

- PostgreSQL is the durable store
- Redis is the fast path for hot lookups and counters

## Main User Flows

### 1. Open the app

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant F as Frontend
  participant B as Backend
  participant DB as PostgreSQL

  U->>F: Open the site
  F->>B: GET app shell and initial API calls (sync)
  B->>DB: load initial state if needed (sync)
  DB-->>B: initial data
  B-->>F: page data / auth state
  F-->>U: render dashboard or landing page
```

### 2. Sign in or refresh session

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant F as Frontend
  participant B as Backend
  participant DB as PostgreSQL
  participant R as Redis

  U->>F: Submit login form
  F->>B: POST /api/v1/auth/login (sync)
  B->>DB: validate credentials (sync)
  B->>DB: persist refresh-token state (sync)
  B->>R: cache refresh metadata if enabled (sync)
  B-->>F: JWT + refresh cookie
  F-->>U: update auth state
```

Refresh flow:

```mermaid
sequenceDiagram
  autonumber
  participant F as Frontend
  participant B as Backend
  participant R as Redis
  participant DB as PostgreSQL

  F->>B: POST /api/v1/auth/refresh (sync)
  B->>R: lookup refresh token metadata first (sync)
  alt cache hit
    R-->>B: token metadata
  else cache miss
    B->>DB: load refresh token state (sync)
    DB-->>B: token row
    B->>R: repopulate cache (sync)
  end
  B-->>F: new access token and rotated refresh cookie
```

### 3. Create a short link

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant F as Frontend
  participant B as Backend
  participant DB as PostgreSQL
  participant R as Redis
  participant A as Analytics

  U->>F: Submit create-link form
  F->>B: POST /api/v1/urls (sync)
  B->>DB: validate and persist link (sync)
  B->>R: warm hot lookup cache (sync)
  B-->>A: publish LinkCreatedEvent (async)
  B-->>F: short-link response
  F-->>U: show created link and QR
```

### 4. Open a short link

```mermaid
sequenceDiagram
  autonumber
  participant U as Visitor
  participant F as Browser
  participant B as Backend
  participant R as Redis
  participant DB as PostgreSQL
  participant A as Analytics

  U->>F: Open /r/{code}
  F->>B: GET /r/{code} (sync)
  B->>R: lookup hot code (sync)
  alt cache hit
    R-->>B: short-link data
  else cache miss
    B->>DB: load link row (sync)
    DB-->>B: link data
    B->>R: store hot cache entry (sync)
  end
  B-->>A: publish LinkClickedEvent (async)
  B-->>F: redirect response
  F-->>U: browser follows redirect target
```

### 5. View analytics

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant F as Frontend
  participant B as Backend
  participant DB as PostgreSQL

  U->>F: Open analytics dashboard
  F->>B: GET /api/v1/analytics/{code} (sync)
  B->>DB: read summary model (sync)
  DB-->>B: analytics summary
  B-->>F: dashboard-ready response
  F-->>U: render stats and charts
```

## Frontend To Backend Contract

The frontend should only talk to backend HTTP endpoints.

It should not:

- access the database directly
- know internal backend repositories
- depend on Redis directly
- depend on backend package internals

That keeps the browser layer simple and the backend boundaries enforceable.

## Backend To Infrastructure Contract

### PostgreSQL

Used for:

- identity and refresh-token state
- links and ownership
- click events and summaries
- verification and reset tokens

### Redis

Used for:

- hot redirect lookups
- rate limiting counters
- optional refresh-token lookup acceleration

### Events

Used for:

- link creation notifications
- click analytics fan-out
- future background jobs that do not need to block the user

## Sync Vs Async Summary

### Synchronous

- browser to backend HTTP calls
- backend service calls
- repository reads and writes
- cache reads and writes
- login, refresh, logout, create-link, redirect, and analytics read paths

### Asynchronous

- link-created events to analytics
- link-clicked events to analytics
- future background processing that should not block the request path

## Practical Rule Of Thumb

- If the user is waiting on the response, the flow is synchronous.
- If the backend publishes a domain event for a later consumer, the flow is asynchronous.
- If the data must survive restarts, keep it in PostgreSQL.
- If the data only speeds up hot paths, keep it in Redis.
