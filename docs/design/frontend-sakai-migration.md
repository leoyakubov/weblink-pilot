# Frontend Sakai Migration Plan

## Purpose

This document is the working plan for moving the frontend from the current custom visual layer to a full Sakai-based presentation system.

The goal is to keep the app behavior stable while making the visual system more consistent, easier to extend, and closer to a production-style Vue admin/dashboard UI.

## Migration Goals

- move toward full Sakai adoption without a big-bang rewrite
- keep the current design available as a rollback path until the migration is stable
- expose a visible legacy/Sakai switch while the rollout is still being validated
- preserve the existing feature behavior while changing presentation only
- reduce custom CSS where Sakai and PrimeVue already provide a good default
- make the layout, spacing, and hierarchy feel more like a cohesive product UI
- keep the frontend mobile-first during the transition

## Current Baseline

The frontend currently uses:

- Vue 3 with TypeScript
- PrimeVue controls for core inputs and buttons
- a custom shell, spacing system, and page styling
- feature-oriented page folders for the main screens
- a theme toggle that can switch between the legacy dark shell and the lighter Sakai-style shell
- a shared analytics summary component for repeated dashboard/detail blocks
- shared utilities for repeated behavior

That means the migration is mostly about presentation, structure, and shared styling, not business logic.

## Rollback Strategy

We keep the current design as a safety net in three ways:

1. Keep the current visual system in git history as the rollback baseline.
2. Introduce the new Sakai presentation behind a controlled theme path.
3. Preserve the current CSS and structural wrappers until the new version is verified.

If the Sakai rollout does not fit the product, we can fall back by:

- switching the theme flag back
- restoring the previous styling path
- reverting only the latest presentation commits if needed

The current implementation already ships that switch in the app shell, so validation can happen without losing the legacy presentation.

## Migration Phases

### Phase 1 - Theme Foundation

Goals:

- add a clear theme switch between the current look and Sakai
- define the token and styling entry points for the new theme
- keep the existing design available during rollout

Tasks:

- add a theme mode flag
- separate legacy styles from Sakai-driven styles
- keep shared layout primitives theme-neutral where possible
- keep the toggle visible in the shell so switching is immediate during review

### Phase 2 - Shell and Navigation

Goals:

- move the app shell to Sakai-style structure
- align the top navigation, spacing, and section headers
- keep routing and auth behavior unchanged

Tasks:

- restyle the app header and nav
- align the main container, cards, and action rows with Sakai
- keep the current page flow intact

### Phase 3 - Core Pages

Goals:

- migrate the main user-facing pages one by one
- verify each page after the visual change

Suggested order:

1. About
2. Monitoring
3. Home
4. Auth and recovery flows
5. Dashboard, history, and link details

### Phase 4 - Shared UI Cleanup

Goals:

- remove wrapper and compatibility layers once the new style is stable
- unify shared frontend conventions
- reduce duplicated presentation code

Tasks:

- remove remaining wrapper and compatibility layers
- unify `lib` vs `shared/services`
- extract repeated UI blocks from the larger pages into smaller components

Current status:

- legacy copy-button wrappers have been removed from the active frontend path
- the app shell now exposes a legacy/Sakai theme toggle
- the analytics summary block is now a shared component used by dashboard and link detail
- the main pages use PrimeVue buttons directly for copy and action flows

### Phase 5 - Legacy Cleanup

Goals:

- remove the old design path after the Sakai version is stable
- document the final visual system

Tasks:

- delete obsolete CSS rules
- remove unused style tokens
- document the final component and layout rules

## What Will Change

The migration is expected to change:

- shell structure
- card spacing and hierarchy
- typography scale
- button and form layout patterns
- dashboard-like presentation
- global theme variables

## What Should Stay Stable

The migration should not change:

- route paths
- backend integration
- auth behavior
- link creation behavior
- testing expectations
- mobile-first responsiveness

## Verification Plan

After each step:

- run frontend lint
- run frontend tests
- run frontend build
- check the affected pages in the browser

No presentation step should be considered complete unless the gate is green.

## Exit Criteria

The Sakai migration is complete when:

- the app uses the new Sakai-based visual system consistently
- the legacy design path is no longer needed
- the shared UI is smaller and more maintainable
- the current frontend behavior still matches the product flows
- the visual direction is documented well enough for future pages

## Related Docs

- [Frontend Plan](./frontend-plan.md)
- [Roadmap](../planning/roadmap.md)
