# WebLinkPilot Product Spec

## 1. Product Summary

WebLinkPilot is a modern URL shortening platform with QR code generation, analytics, and a mobile-first web interface.

The product is designed to look and feel like a real production service rather than a toy demo.

## 2. Target Users

- developers and engineers who want a portfolio-grade service
- small teams that need branded short links
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

- As a user, I want to view my link history.
- As a user, I want to edit or disable links if needed later.

## 4. Feature Scope

### MVP

- create short URL with random default code
- optional custom alias
- expiration time
- redirect endpoint
- QR code generation
- basic analytics
- mobile-first UI
- authentication for management endpoints

### V1+

- link list and detail pages
- chart-based analytics dashboard
- copy/share actions
- QR download in PNG/SVG
- rate limiting
- cache-backed redirects

### Future

- team accounts
- link ownership
- branded domains
- bulk link generation
- API keys
- webhook delivery for click events
- richer auth/account management that is still not shipped:
  - GitHub social login
  - account management UI/API
  - multiple auth providers if the product needs them

## 5. Frontend Scope

### Pages

- landing/create link page
- short link success page
- QR preview/download page
- analytics dashboard
- link history page
- settings/auth page

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
