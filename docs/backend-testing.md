# Backend Testing Strategy

This project uses a layered testing approach for the Spring Boot modular monolith.

## Test Types

### Unit tests

Use these for small, deterministic pieces of logic:

- codecs and parsers
- request-context helpers
- domain-style calculations
- pure services with mocks

Examples in this repo:

- `backend/url/src/test/java/io/weblinkpilot/url/service/Base62CodecTest.java`
- `backend/url/src/test/java/io/weblinkpilot/url/web/CountryResolverTest.java`
- `backend/analytics/src/test/java/io/weblinkpilot/analytics/service/UserAgentParserTest.java`

### Slice tests

Use these when you want the web or persistence layer with only part of Spring loaded:

- controller tests with `MockMvc`
- security configuration tests
- repository-focused checks

These are useful when the full application context is too expensive or when you want faster feedback around a specific layer.

### Testcontainers-backed integration tests

Use these when you want a production-like PostgreSQL database instead of H2:

- Flyway runs against a real PostgreSQL container
- JPA and repository mappings are exercised against the same database family used in production
- useful for catching SQL dialect or migration differences that H2 can hide

Example in this repo:

- `backend/app/src/test/java/io/weblinkpilot/config/testcontainers/PostgresUrlApiIntegrationTest.java`

### Integration tests

Use these to verify module wiring and real runtime behavior:

- Spring Boot startup
- database and Flyway migrations
- security and filters
- redirect flows
- analytics persistence
- rate limiting

Examples in this repo:

- `backend/app/src/test/java/io/weblinkpilot/ApplicationContextTest.java`
- `backend/app/src/test/java/io/weblinkpilot/url/web/UrlApiIntegrationTest.java`
- `backend/app/src/test/java/io/weblinkpilot/analytics/web/AnalyticsApiIntegrationTest.java`
- `backend/app/src/test/java/io/weblinkpilot/config/rate/RateLimitIntegrationTest.java`
- `backend/app/src/test/java/io/weblinkpilot/config/observability/ObservabilityIntegrationTest.java`

### Contract tests

Use these when the API shape matters for frontend or external consumers:

- request/response schema expectations
- OpenAPI examples
- backward compatibility checks

This repo already keeps the API contract documented in `docs/api-contract-v1.md`.

### Smoke tests

Use these for very fast end-to-end confidence:

- backend health
- frontend loads
- create-link flow
- redirect flow
- Docker stack boot check

This repo has a browser smoke test for the Docker stack in `frontend/scripts/smoke-docker.mjs`.

### Architectural tests

Use these to keep module boundaries healthy:

- package boundary rules
- forbidden dependencies
- no accidental cross-module coupling

These are optional but useful once the modular monolith grows further.

Example in this repo:

- `backend/app/src/test/java/io/weblinkpilot/architecture/ArchitectureTest.java`

## Coverage

The backend build now produces:

- module-level coverage checks for `url`, `analytics`, and `app`
- an aggregate multi-module report in `backend/coverage`

Run the backend test suite from the repo root:

```powershell
.\scripts\backend\test-backend.ps1
```

Run the coverage build from the repo root:

```powershell
.\scripts\backend\coverage.ps1
```

Run the aggregate coverage build from `backend/`:

```powershell
.\mvnw.cmd clean verify
```

The HTML report is generated under:

- `backend/coverage/target/site/jacoco-aggregate/index.html`

## SonarQube

The repo also has a local SonarQube stack under `infra/sonar/`.

Start it from the repo root:

```powershell
.\scripts\sonar\run-sonar-stack.ps1
```

Then run the analysis from `backend/` with a Sonar token:

```powershell
$env:SONAR_TOKEN = "<your-token>"
.\mvnw.cmd clean verify sonar:sonar -Dsonar.token=$env:SONAR_TOKEN
```

The backend POM points Sonar at the aggregate JaCoCo XML report:

- `coverage/target/site/jacoco-aggregate/jacoco.xml`

## Current Rule of Thumb

The goal is not to test every line equally.

The goal is to cover:

- business rules
- module boundaries
- cross-module behavior
- startup and production concerns

That gives the highest return for a modular Spring Boot backend.
