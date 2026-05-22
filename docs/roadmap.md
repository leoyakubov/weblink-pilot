# Roadmap

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
