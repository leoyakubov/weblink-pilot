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
- Thymeleaf text templates for account emails
- springdoc-openapi 3.0.3

### Data stores

- PostgreSQL 16
- Redis 7
- H2 for tests and the optional ephemeral hosted demo

### Testing

- JUnit 5
- Mockito
- Testcontainers

### Build tool

- Maven

### Backend modules

- `shared`
- `auth`
- `links`
- `analytics`
- `ai`
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
- Vitest
- Playwright

### Styling

- PrimeVue controls
- PrimeVue Nora theme preset
- Custom CSS visual system

Why:

- PrimeVue gives us stable Vue 3 controls for forms, buttons, drawers, and common action patterns.
- The Nora theme preset provides the PrimeVue token baseline used by those controls.
- The app presentation stays custom and product-specific instead of adopting a Sakai template migration.
- Custom CSS tokens and shared components keep the UI flexible without locking us into a template.

## Infra and delivery

- Docker
- Docker Compose
- GitHub repository
- local env files with examples

## Versioning policy

We pin exact versions in the repo where it helps reproducibility, and we keep the stack aligned with the current stable releases used by the project.

Current baseline:

- backend on Java 21 + Spring Boot 4.0.6 + Thymeleaf email templates + springdoc-openapi 3.0.3
- frontend on Vue 3.5.34 + Vue Router 4.6.4 + Vite 8.0.14 + Node 24.16.0 LTS + npm 11.13.0
- styling on plain CSS
