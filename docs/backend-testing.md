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
- live deployment smoke checks

This repo has a browser smoke test for the Docker stack in `frontend/scripts/smoke-docker.mjs`.
For local Docker smoke, use `scripts/win/quality/deployment-smoke.ps1` or `scripts/unix/quality/deployment-smoke.sh` with the default local targets.
For the deployed demo, set `SMOKE_TARGET=demo` and provide `RENDER_HEALTH_URL` and `FRONTEND_SMOKE_URL`.
The smoke output prints the backend HTTP status and `status=UP`, plus the frontend HTTP status and the app shell marker (`id="app"`).

### Auth session workflow

Use this when you want to verify the access-token and refresh-cookie flow end to end:

- login and registration
- access-token storage in session storage
- automatic refresh on `401`
- refresh-cookie rotation
- logout revocation

The full local workflow is documented in:

- [`docs/auth-testing.md`](auth-testing.md)

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
- a dedicated Maven `ci` profile for verification runs in CI and local quality gates

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
.\mvnw.cmd -Pci clean install
```

The HTML report is generated under:

- `backend/coverage/target/site/jacoco-aggregate/index.html`

## SonarQube

The repo also has a local SonarQube stack under `infra/sonar/`.
GitHub Actions Sonar is temporarily disabled for now; use the local stack and helper script below.

Start it from the repo root:

```powershell
.\scripts\win\quality\sonar-stack.ps1
```

Then run the analysis from `backend/` with a Sonar token:

```powershell
$env:SONAR_TOKEN = "<your-token>"
$env:SONAR_HOST_URL = "http://localhost:9001"
.\mvnw.cmd -Pci clean install sonar:sonar -Dsonar.token=$env:SONAR_TOKEN -Dsonar.host.url=$env:SONAR_HOST_URL
```

The backend POM points Sonar at the aggregate JaCoCo XML report:

- `coverage/target/site/jacoco-aggregate/jacoco.xml`

For local SonarQube, the scripts and docs default the host URL to `http://localhost:9001`.
For GitHub Actions, set `SONAR_HOST_URL` as a repository secret or environment secret that points at the hosted SonarQube server.

## Dependency Vulnerability Checks

The repo now provides dependency vulnerability checks for both the backend and frontend as a manual security gate:

- Backend: OWASP Dependency-Check on the Maven reactor
- Frontend: `npm audit --audit-level=high`

Run the combined security gate from the repo root when you want to check dependencies manually:

```powershell
.\scripts\win\security\check-dependencies.ps1
```

On macOS/Linux:

```bash
./scripts/unix/security/check-dependencies.sh
```

You can also run the backend and frontend checks separately with the helper scripts listed in the README.

## Secret Scanning

The repo also runs a Gitleaks-based secret scan to catch hardcoded credentials and tokens before they leave the machine or reach CI.

Run it from the repo root:

```powershell
.\scripts\win\git\scan-secrets.ps1
```

On macOS/Linux:

```bash
./scripts/unix/git/scan-secrets.sh
```

## Current Rule of Thumb

The goal is not to test every line equally.

The goal is to cover:

- business rules
- module boundaries
- cross-module behavior
- startup and production concerns

That gives the highest return for a modular Spring Boot backend.
