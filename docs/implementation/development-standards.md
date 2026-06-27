# Development Standards

This document is the working checklist for development hygiene in WeblinkPilot.

## Goal

Keep the codebase predictable, reviewable, and hard to accidentally break.

## Must Have

- Automated formatting for Java and frontend code.
- Tests for backend and frontend logic.
- Coverage checks for backend and frontend in CI or manual runs.
- CI that runs the same checks as local quality gates.
- Pre-push checks for fast feedback before code leaves the machine.
- Centralized config and secret handling.
- Secret scanning for hardcoded credentials and tokens.
- Clear module boundaries.
- Verification must be executed before claiming a change is working.

## Should Have

- Java formatter and import ordering.
- Frontend linting and formatting.
- Static analysis for common bugs and risky patterns.
- Dependency vulnerability checks.
- Architecture tests for module boundaries.
- Smoke checks for the full app flow.
- Deployment smoke tests.
- Release notes or changelog entries.
- Consistent logging format.

## Nice To Have

- Dependency update automation.
- Secret scanning.

## Current Quality Flow

1. Format code.
2. Run tests.
3. Run build checks.
4. Run secret scans.
5. Run static analysis.
6. Push only if the pre-push gate passes.
7. Run coverage checks in CI or manually when needed.

## Verification Rule

- After any code or behavior change, run the relevant verify or build command in the current workspace before telling others that the change works.
- If the exact scope is unclear, prefer the broader repo or backend/frontend verify command over a narrow guess.
- If verification fails, report the failure plainly and do not describe the change as passing.
- Docs-only changes still need a quick sanity check such as `git diff --check`, but they do not need a full build unless the docs include generated artifacts or live links that must be validated.

## Tooling Plan

- Backend:
  - formatter
  - lint/static analysis
  - dependency vulnerability checks
  - architecture checks
  - coverage in CI/manual runs
- Frontend:
  - formatter
  - lint/static analysis
  - TypeScript compile check
  - coverage in CI/manual runs
- Repo:
  - pre-push wrapper
  - CI job parity
  - dependency vulnerability scanning
  - secret scanning

## Notes

- Formatting should be automatic and low friction.
- Linting should catch bugs and code smells, not police personal style.
- Static analysis should be strict enough to help, but not so noisy that it gets ignored.
