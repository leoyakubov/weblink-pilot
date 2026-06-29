# WeblinkPilot Product Spec

## 1. Product Summary

WeblinkPilot is a modern URL shortening platform with QR code generation, analytics, and a mobile-first web interface.

The product is designed to look and feel like a real production service rather than a toy demo.

## 2. Target Users

- developers and engineers who want a portfolio-grade service
- individual users who want clean personal short links, saved history, and QR sharing
- users sharing links through mobile and QR
- hiring managers evaluating backend and product thinking

## 3. Core User Stories

### URL creation

- As a user, I want a short URL to be generated automatically.
- As a user, I want to optionally set a custom alias.
- As a user, I want to set expiration.
- As a user, I want to generate a QR code for the short URL.

### Link usage

- As a user, I want to open a short link and be redirected quickly.
- As a user, I want QR scanning to land on the same redirect flow.

### Analytics

- As a user, I want to see click counts.
- As a user, I want to see recent clicks.
- As a user, I want to understand device, browser, referrer, and geo trends.

### Management

- As a user, I want to view my saved links.
- As a user, I want to open a link detail page with QR, copy, share, and analytics actions.
- As a user, I want to edit or disable links if needed later.
- As an admin, I want to review system health, configuration, runtime metrics, and registered users.

## 4. Feature Scope

### MVP

- [x] create short URL with random default code
- [x] optional custom alias
- [x] expiration time
- [x] redirect endpoint
- [x] QR code generation
- [x] basic analytics
- [x] mobile-first UI
- [x] authentication for management endpoints

### V1+

- [x] link list and detail pages
- [x] chart-based analytics dashboard
- [x] copy/share actions
- [x] QR download in PNG/SVG
- [x] rate limiting
- [x] cache-backed redirects
- [x] security headers, strict CORS validation, deployment-safe operational metrics, and dedicated auth throttling
- [x] optional Cloudflare Web Analytics for public demo traffic
- [x] AI-style link metadata enrichment with a free deterministic stub provider

### Future

- [ ] team accounts
- [x] link ownership
- [ ] branded domains
- [ ] bulk link generation
- [ ] API keys
- [ ] webhook delivery for click events
- [ ] additional auth/account extensions if the product needs them:
  - [ ] more identity providers
  - [ ] organization-level account features
  - [ ] team permissions
  - [ ] remember-me session controls for trusted devices

## 5. Frontend Scope

### Pages

- `/` - Home and create-link flow
- `/links` - Links list with filters and quick actions
- `/link/:code` - Link details, QR code, copy/share/open actions, and JSON preview
- `/analytics` - Analytics overview across visible links
- `/analytics/:code` - Per-link analytics detail page
- `/account` - Account profile, password/security actions, and identity provider information
- `/about` - Product, access, seeded data, stack, implementation, API endpoints, and project links
- `/monitoring` - Admin monitoring with health checks, runtime metrics, configuration, and service endpoints
- `/admin/users` - Admin read-only users directory
- `/auth/signin` and `/auth/signup` - Authentication forms
- `/auth/forgot-password`, `/auth/reset-password`, `/auth/verify-email/request`, and `/auth/verify-email` - Account recovery and email verification flows
- `/auth/github/complete` - GitHub OAuth completion and error handoff
- `/settings/reset` - Browser settings reset utility

### Design goals

- mobile-first
- minimal steps to create and share a link
- strong visual hierarchy
- quick QR access
- responsive layout for desktop and mobile

## 6. QR Code Requirements

QR is not an extra asset.

It is part of the core user journey:

- user creates a short link
- system generates QR
- user scans QR from a phone
- request follows the same redirect path

Output formats to support:

- PNG
- SVG

## 7. Analytics Requirements

Track:

- total clicks
- unique visitors by IP or hashed identity
- user-agent
- browser family
- device type
- referrer
- geo approximation

Analytics should be:

- asynchronous
- non-blocking for redirect latency
- ready for future extraction into a separate service

## 8. Broker Decision

Recommended approach for v1:

- no external broker yet
- internal domain events only
- prepare the contract for future Kafka or RabbitMQ integration

Recommended broker later:

- Kafka if we want event-stream and analytics story
- RabbitMQ if we want simpler queues and retries

For this project, Kafka is the stronger long-term fit, but it should be introduced only when there is a clear operational reason.

## 9. Repository Strategy

Recommended structure:

- one GitHub monorepo
- backend in one folder
- frontend in one folder
- docs and infra in separate folders

Why:

- easier to coordinate API changes
- simpler local setup
- better for a solo portfolio project
- easier to keep implementation and documentation aligned

## 10. Success Criteria

The project is successful if it can demonstrate:

- thoughtful architecture
- backend depth
- modern frontend delivery
- event-driven design
- production readiness
- a clear story for interviews
