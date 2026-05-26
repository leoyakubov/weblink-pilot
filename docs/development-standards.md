# Development Standards

This document is the working checklist for development hygiene in WebLinkPilot.

## Goal

Keep the codebase predictable, reviewable, and hard to accidentally break.

## Must Have

- Automated formatting for Java and frontend code.
- Tests for backend and frontend logic.
- Coverage gates for backend and frontend.
- CI that runs the same checks as local quality gates.
- Pre-push checks for fast feedback before code leaves the machine.
- Centralized config and secret handling.
- Secret scanning for hardcoded credentials and tokens.
- Clear module boundaries.

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
3. Run coverage checks.
4. Run build checks.
5. Run secret scans.
6. Run static analysis.
7. Push only if the pre-push gate passes.

## Tooling Plan

- Backend:
  - formatter
  - lint/static analysis
  - dependency vulnerability checks
  - architecture checks
- Frontend:
  - formatter
  - lint/static analysis
  - TypeScript compile check
- Repo:
  - pre-push wrapper
  - CI job parity
  - dependency vulnerability scanning
  - secret scanning

## Notes

- Formatting should be automatic and low friction.
- Linting should catch bugs and code smells, not police personal style.
- Static analysis should be strict enough to help, but not so noisy that it gets ignored.
