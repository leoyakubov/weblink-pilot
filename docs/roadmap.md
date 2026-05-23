# Roadmap

## Status Overview

| Status | Area | Notes |
|---|---|---|
| Done | Repo readiness | Repo layout, docs, scripts, Maven wrapper, Docker, Sonar, coverage, and test tooling are in place. |
| Done | Backend foundation | Modular backend, persistence, security, cache, actuator, logging, and observability are in place. |
| Done | URL lifecycle | Create, read, redirect, custom alias, expiration, preview, and QR flows are implemented. |
| Done | Analytics | Click events are tracked by source (redirect and QR), with summaries and enrichment. |
| Done | Frontend foundation | Vue app shell and backend integration are in place. |
| Done | Frontend feature set | Create flow, dashboard, history, details, and QR UI are in place. |
| Done | Hardening baseline | Tests, coverage, rate limiting, Docker, Sonar, ArchUnit, and Testcontainers are in place. |
| Done | Deployment setup | CI and deploy workflows are in place for the live Netlify frontend and Render backend. |
| Next | Monitoring admin page | Add an internal monitoring page plus Prometheus/Grafana links and local monitoring stack now that the app is deployed. |
| Later | Monitoring auth | Optionally protect monitoring endpoints with JWT after the admin page is in place. |
| Done | Environment profiles | Local and demo Spring profiles are in place, with Maven convenience profiles for direct backend runs. |
| Nice to do | Async broker | Add RabbitMQ or Kafka only if event volume or service separation warrants it. |

## Phase 1 - Planning

- define architecture and product scope
- lock repository layout
- agree on technical stack and version baseline

## Phase 2 - Backend foundation

- create backend monorepo structure
- define shared contracts and domain modules
- add persistence model and initial API contracts
- set up auth, cache, and event boundaries

## Phase 3 - Frontend foundation

- create Vue app with mobile-first UI
- implement link creation flow
- implement QR preview and download flow
- implement analytics dashboard shell

## Phase 4 - Core features

- short URL creation
- custom aliases
- expiration handling
- redirect flow
- QR code generation
- click analytics

## Phase 5 - Production hardening

- integration tests
- observability
- rate limiting
- deployment setup
- documentation polish

## Phase 6 - Future evolution

- introduce broker if needed
- extract analytics if it becomes valuable
- split services only when the boundaries prove themselves
- refine Maven profiles only if they reduce complexity further
