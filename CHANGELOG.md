# Changelog

All notable changes to WeblinkPilot are documented in this file.

The project follows a lightweight Keep a Changelog style:
- release entries summarize user-visible changes, fixes, and notable infrastructure updates.
- internal refactors stay out of release notes unless they affect behavior.

## [0.1.0]

- Initial public baseline for the modular monolith, frontend shell, and deployment pipeline.

## [0.2.0] - 2026-05-26

This release brings the roadmap from bootstrap through monitoring into a shippable shape.

### Added
- Modular backend foundation with persistence, security, caching, observability, and module boundaries.
- URL lifecycle features including creation, redirect, custom aliases, previews, QR output, anonymous demo links, and signed-in owned links.
- Analytics for redirect clicks and QR scans.
- Vue frontend shell, dashboard, history, details, QR UI, and auth screens.
- JWT authentication with user/admin roles, bootstrap seed data, and admin-only monitoring access.
- Local Prometheus and Grafana monitoring for the Docker stack.
- Deployment smoke checks for backend and frontend.

### Changed
- Short links now support optional expiration with a configurable maximum lifetime and archive expired links instead of hard-deleting them.
- Redis-backed hot short-code caching and analytics cache invalidation were introduced.
- Local, dev, and demo environment profiles plus the helper scripts were standardized.
- CI and deployment workflows were added for the Netlify frontend and Render backend.
- README badges and workflow labels were simplified for faster scanning.

### Fixed
- Monitoring overview repository queries now use the correct JPA property path.
- Several CI and shell wrapper issues around PowerShell, executable bits, and smoke-output handling were cleaned up.
