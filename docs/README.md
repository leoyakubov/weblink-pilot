# Documentation Index

This repo keeps one documentation tree with a small set of categories so it is easier to find the right document quickly.

## Planning & Requirements

- [Product Spec](planning/product-spec.md) - product goals, scope, and user stories.
- [Roadmap](planning/roadmap.md) - the single planning source of truth and release sequencing.

## Design & Architecture

- [Architecture Plan](design/architecture-plan.md) - system shape, runtime composition, and module boundaries.
- [Backend Module Plan](design/backend-module-plan.md) - backend modules, dependencies, and data flow.
- [Frontend Plan](design/frontend-plan.md) - Vue app structure, pages, components, and state plan.
- [Architecture Decisions](design/adr.md) - the decisions we have already accepted and why.
- [Tech Stack](design/tech-stack.md) - the approved baseline stack for backend, frontend, and infrastructure.
- [Repository Structure](design/repo-structure.md) - the intended folder layout for the monorepo.

## Implementation & Development

- [API Contract v1](implementation/api-contract-v1.md) - the agreed backend/frontend contract surface.
- [Development Standards](implementation/development-standards.md) - formatting, testing, coverage, scanning, and release hygiene.

## Testing & QA

- [Feature Testing Guide](testing/feature-testing.md) - feature-by-feature manual verification matrix.
- [Auth Testing Workflow](testing/auth-testing.md) - the detailed auth, refresh, and cookie workflow.
- [Backend Testing Strategy](testing/backend-testing.md) - backend test types, quality gates, and coverage guidance.

## Deployment & Operations

- [Deployment](operations/deployment.md) - local/demo deployment shape, secrets, and workflows.

## Release & Reference

- [Changelog](../CHANGELOG.md) - release notes for shipped user-visible changes.
- [Interview Notes](reference/interview-notes.md) - short talking points and trade-offs for interviews.

## Suggested Reading Order

1. Start with [Product Spec](planning/product-spec.md) to understand the product shape.
2. Read [Architecture Plan](design/architecture-plan.md) and [Backend Module Plan](design/backend-module-plan.md) for system design.
3. Use the [Feature Testing Guide](testing/feature-testing.md) and [Auth Testing Workflow](testing/auth-testing.md) when you need verification steps.
4. Check [Deployment](operations/deployment.md) and [Development Standards](implementation/development-standards.md) for runtime and hygiene expectations.
