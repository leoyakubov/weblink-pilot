# Tech Stack

## Backend

### Runtime

- Java 21 LTS

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
- springdoc-openapi

### Data stores

- PostgreSQL 16
- Redis 7

### Testing

- JUnit 5
- Mockito
- Testcontainers

### Build tool

- Maven

## Frontend

### Runtime

- Node.js 24.16.0 LTS

Why:

- current LTS line according to the official Node.js release schedule
- safe choice for a production-style frontend toolchain

### Framework

- Vue 3.5.x

Why:

- latest major Vue line
- official docs and ecosystem recommend Vue 3 for new projects

### Frontend toolchain

- Vite 8.x
- TypeScript
- Vue Router
- Pinia
- Vitest
- Playwright

### Styling

- Tailwind CSS 4.3.x

Why:

- modern CSS-first setup
- well-suited for a fast, mobile-first UI

## Infra and delivery

- Docker
- Docker Compose
- GitHub repository
- local env files with examples

## Versioning policy

We will pin exact versions in the repo once implementation starts.

For planning purposes, the target baseline is:

- backend on Java 21 + Spring Boot 4.0.6
- frontend on Vue 3.5.x + Vite 8.x + Node 24 LTS
- styling on Tailwind CSS 4.3.x

