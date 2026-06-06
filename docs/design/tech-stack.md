# Tech Stack

## Backend

### Runtime

- Java 25 LTS

Why:

- modern LTS
- strong Spring Boot support
- enables virtual threads if we want them later

### Framework

- Spring Boot 4.0.6

Why:

- current stable Spring Boot line in the official docs
- supports modern Java and a broad production ecosystem

### Backend libraries

- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Cache
- Spring Actuator
- Flyway
- springdoc-openapi 3.0.3

### Data stores

- PostgreSQL 16
- Redis 7

### Testing

- JUnit 5
- Mockito
- Testcontainers

### Build tool

- Maven

### Backend modules

- `shared-contracts`
- `auth`
- `links`
- `analytics`
- `application`
- `build-support`

## Frontend

### Runtime

- Node.js 24.16.0 LTS
- npm 11.13.0

Why:

- current LTS line according to the official Node.js release schedule
- current npm version bundled with Node.js 24.16.0 LTS
- safe choice for a production-style frontend toolchain

### Framework

- Vue 3.5.34

Why:

- latest major Vue line
- official docs and ecosystem recommend Vue 3 for new projects

### Frontend toolchain

- Vue Router 4.6.4
- Vite 8.0.14
- TypeScript 5.9.3
- Pinia
- Vitest
- Playwright

### Styling

- PrimeVue
- Sakai template
- CSS overrides

Why:

- PrimeVue gives us a rich Vue 3 component set for tables, forms, dialogs, menus, and dashboard blocks
- Sakai provides a free, ready-made Vue 3 admin/dashboard template that fits our demo style
- CSS overrides keep the UI flexible without locking us into a heavy custom design system

## Infra and delivery

- Docker
- Docker Compose
- GitHub repository
- local env files with examples

## Versioning policy

We pin exact versions in the repo where it helps reproducibility, and we keep the stack aligned with the current stable releases used by the project.

Current baseline:

- backend on Java 25 + Spring Boot 4.0.6 + springdoc-openapi 3.0.3
- frontend on Vue 3.5.34 + Vue Router 4.6.4 + Vite 8.0.14 + Node 24.16.0 LTS + npm 11.13.0
- styling on plain CSS
