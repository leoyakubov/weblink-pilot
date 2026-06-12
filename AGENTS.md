# AGENTS.md

Repository instructions for coding agents working on WebLinkPilot.

This file is intentionally agent-focused. Human-facing project docs stay in `README.md` and `docs/`.

## Project Context

- WebLinkPilot is a monorepo with a Java/Spring Boot modular backend and a Vue frontend.
- The backend is a modular monolith under `backend/`:
  - `shared-contracts` contains DTOs and cross-module contracts.
  - `url` owns short-link lifecycle behavior.
  - `analytics` owns click/event analytics behavior.
  - `app` wires runtime, security, persistence, bootstrap, and HTTP controllers.
- The frontend lives under `frontend/` and is a Vue SPA.
- Platform scripts are split by OS:
  - Windows: `scripts/win/...`
  - Unix/macOS/Linux: `scripts/unix/...`

## Source Of Truth

- Planning and delivery order: `docs/planning/roadmap.md`.
- API contract: `docs/implementation/api-contract-v1.md`.
- Development rules and local commands: `docs/implementation/development-environment.md`.
- Test workflows: `docs/testing/`.
- Deployment/runtime notes: `docs/operations/deployment.md`.
- Security risks and follow-ups: `docs/reference/security-review.md`.

Update the relevant doc when a change becomes part of the durable project baseline.

## Working Style

- Start by reading the relevant files and current docs before changing code.
- Prefer small, reviewable vertical slices over broad refactors.
- Keep backend, frontend, docs, and infra changes separated when practical.
- Do not rewrite unrelated files or reformat broad areas unless the task calls for it.
- If a feature changes roadmap scope, update `docs/planning/roadmap.md`.
- If a feature changes behavior users or operators rely on, update the API, testing, or deployment docs in the same work item.

## Implementation Boundaries

- Keep backend module boundaries intact.
- Do not put URL lifecycle logic into `app` unless it is HTTP/runtime wiring.
- Do not put analytics domain logic into controllers.
- Keep shared contracts stable and update frontend types/tests when contracts change.
- Follow existing Spring Boot, JPA, Flyway, Vue, and script patterns before introducing new abstractions.
- Ask before adding new production dependencies or external services.

## Commands

Prefer project wrapper scripts over ad hoc commands.

Windows:

```powershell
.\scripts\win\run-before-push.ps1
.\scripts\win\dev\docker-full-stack.ps1
.\scripts\win\quality\deployment-smoke.ps1
.\scripts\win\security\check-dependencies.ps1
```

Unix/macOS/Linux:

```bash
./scripts/unix/run-before-push.sh
./scripts/unix/dev/docker-full-stack.sh
./scripts/unix/quality/deployment-smoke.sh
./scripts/unix/security/check-dependencies.sh
```

Backend targeted tests from `backend/`:

```powershell
.\mvnw.cmd -pl applicationlication -am "-Dtest=SomeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Frontend targeted tests from `frontend/`:

```bash
npm run test:run -- src/path/to/test.ts
```

## Verification Policy

- Backend change: run the smallest Maven test slice that covers the changed code, plus style when formatting or imports changed.
- Frontend change: run the affected Vitest tests, then lint/build when user-facing UI or shared client code changed.
- Cross-surface change: verify both backend and frontend.
- Frontend-only change: rerun frontend checks only.
- Backend-only change: rerun backend checks only.
- Backend and frontend change together: rerun the full verify flow.
- Docs-only change: run `git diff --check`; no build is required unless links or generated docs are involved.
- Before committing, run `scripts/*/run-before-push.*` when the change is broad or touches shared behavior.
- After any code or behavior change, do not claim the change works until the relevant verification command has been run in the current workspace and has passed.
- If you make additional edits after a successful verification run, rerun verification before describing the final state or committing.
- When the exact scope is unclear, prefer the broader project verify command over a narrow guess, then report the exact command and result back to the user.
- If verification fails, report the failure plainly and do not describe the change as working.

If a command cannot run because of sandbox, network, Docker, or local tool limits, report that clearly and run the closest meaningful check.

## Security Rules

- Never hardcode secrets, API keys, personal tokens, real SMTP credentials, or private URLs.
- Keep local secrets in untracked local files such as `.env.local`.
- Do not log password reset links, verification links, refresh tokens, access tokens, or OAuth tickets.
- Keep refresh tokens and account action tokens hashed at rest.
- Review `docs/reference/security-review.md` before changing auth, CORS, cookies, rate limiting, or actuator exposure.
- Treat public endpoints and demo settings as production-visible unless the docs explicitly say otherwise.

## Git And Commits

- Do not push to GitHub without explicit user approval.
- Use concise Conventional Commit messages unless the user asks for another style.
- Do not amend commits unless explicitly requested.
- Do not revert user changes unless the user explicitly asks for that exact revert.
- Commit only after relevant verification has run or after clearly documenting why a check could not run.

## Documentation Hygiene

- Keep README concise and link deeper docs instead of duplicating them.
- Keep long-lived workflow rules in this file or `docs/implementation/development-environment.md`.
- Keep testing walkthroughs in `docs/testing/`.
- Keep security findings in `docs/reference/security-review.md`.
- Keep release-facing summaries in `CHANGELOG.md`.
