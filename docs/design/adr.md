# Architecture Decision Records

## ADR-001: Start with a modular monolith

Status: accepted

### Context

We need a portfolio-grade URL shortener that can be delivered quickly but still demonstrate system design maturity.

### Decision

Start with a modular monolith instead of multiple microservices.

### Consequences

- faster delivery
- lower operational complexity
- easier local development
- clear module boundaries still allow future extraction
- analytics can later be split into a separate service if traffic or scope justifies it

## ADR-002: Use event-driven boundaries inside the monolith

Status: accepted

### Context

We want backend modules to behave like independently evolvable services without introducing broker complexity too early.

### Decision

Modules communicate through domain events and application events rather than direct cross-module service calls.

### Consequences

- redirect path stays clean and fast
- analytics stays decoupled from link resolution
- future Kafka or RabbitMQ integration becomes a wiring change rather than a redesign
- we can later introduce an outbox and external broker if needed

## ADR-003: Use one monorepo for backend, frontend, docs, and infra

Status: accepted

### Context

This is a solo portfolio project with shared product decisions across backend and frontend.

### Decision

Keep everything in one GitHub repository.

### Consequences

- easier coordination between API and UI
- simpler CI/CD and local setup
- docs stay close to implementation
- repo is easier to present during interviews

## ADR-004: Keep broker integration for a later phase

Status: accepted

### Context

We need an event-driven design story, but we do not yet need distributed infrastructure overhead.

### Decision

Do not introduce Kafka or RabbitMQ in v1. Use internal events first, with an architecture that can adopt a broker later.

### Consequences

- avoids unnecessary infrastructure in the MVP
- keeps the system easier to run locally
- preserves a clean migration path to a broker-backed analytics pipeline

