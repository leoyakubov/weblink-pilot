# Implementation Checklist

## Purpose

This checklist turns the planning documents into an execution order.

It helps us build the project in small, verifiable slices without jumping too early into deep implementation details.

## Phase 0 - Repo readiness

Goals:

- final repo naming
- baseline docs in place
- clean `.gitignore`
- repo structure agreed

Exit criteria:

- planning docs committed
- README links updated
- tech stack finalized

## Phase 1 - Backend foundation

Goals:

- create backend module structure
- wire Spring Boot application
- configure PostgreSQL, Flyway, security, cache, and basic observability
- define shared contracts and domain modules

Exit criteria:

- backend starts locally
- database migrations run successfully
- health endpoint is available
- module boundaries are visible in code

## Phase 2 - URL lifecycle

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

## Phase 3 - QR code support

Goals:

- generate QR code for a short URL
- expose QR endpoint
- make QR visible in the API contract and frontend

Exit criteria:

- backend can generate QR output
- frontend can display or download QR

## Phase 4 - Analytics

Goals:

- publish click events
- consume events asynchronously
- persist click data
- expose analytics summary endpoints

Exit criteria:

- redirect path emits events
- analytics are stored and queryable
- metrics are visible for one short code

## Phase 5 - Frontend foundation

Goals:

- scaffold Vue app
- create mobile-first shell
- wire routing and base layout
- connect to backend API

Exit criteria:

- frontend loads and navigates
- create-link form is present
- API integration is working

## Phase 6 - Frontend feature set

Goals:

- create link flow UI
- success page with short link and QR
- analytics dashboard
- link details/history views

Exit criteria:

- core user journeys work on mobile and desktop
- UI is stable enough for demo and review

## Phase 7 - Production hardening

Goals:

- integration and end-to-end tests
- rate limiting
- logging and metrics
- Docker deployment
- documentation polish

Exit criteria:

- project runs in containers
- tests cover key flows
- README and docs describe how to use it

## Suggested build order

1. backend foundation
2. URL lifecycle
3. QR support
4. analytics
5. frontend foundation
6. frontend features
7. hardening

