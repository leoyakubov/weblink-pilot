# Security Review

This document tracks the main OWASP-style risks in the current codebase and gives each item a simple status so we can follow up on it over time.

Status meanings:
- `Mitigated` means the current code already addresses the issue reasonably well.
- `Open` means we should still plan a fix.
- `Monitor` means the current shape is acceptable for now, but we should keep reviewing it as the app grows.

| Area | Problem | Possible Solution | Priority | Status |
| --- | --- | --- | --- | --- |
| Password storage | Passwords are hashed before saving, which is correct. The main risk is accidentally bypassing the encoder in future flows. | Keep using `PasswordEncoder` everywhere a password is stored or changed, and keep tests around register, reset, and password change flows. | High | Mitigated |
| CSRF | Cookie-based refresh and logout endpoints are sensitive to CSRF when the frontend and backend are cross-site, especially with `SameSite=None` in demo. | The default refresh cookie policy is `SameSite=Lax`; keep demo/prod same-site when possible. If cross-site cookies become required, add an explicit CSRF token/header flow before enabling `SameSite=None`. | High | Monitor |
| XSS and token theft | Access tokens still live in browser storage, so any XSS bug could steal them. | Avoid `v-html` and raw HTML injection, keep access tokens short-lived, ship CSP/referrer/permissions headers, and consider moving more auth state into cookies or a BFF later. | High | Monitor |
| SQL injection | Current JPA and JPQL queries are parameterized, so the risk is low as long as we avoid string-built SQL. | Keep using bound parameters and repository methods; avoid concatenating user input into SQL or native queries. | Medium | Mitigated |
| Broken access control | Public endpoints like metrics/prometheus and any newly added route can widen exposure if not reviewed. | Metrics and Prometheus require admin access by default; only local/dev profiles open them for the Docker Prometheus scrape. Keep deny-by-default security and review every new route. | High | Mitigated |
| CORS misconfiguration | Loose allowed-origin patterns could let unwanted origins call authenticated APIs. | CORS remains allowlist-driven, credentials are explicit, and wildcard origins are rejected at startup. | High | Mitigated |
| Brute force and abuse | Login, refresh, password reset, and verification endpoints can be spammed or brute-forced. | Public auth endpoints now use a dedicated, stricter rate-limit bucket in addition to the general API and redirect buckets. | Medium | Mitigated |
| Sensitive logging | Reset and verification links must not leak from normal operational logs, and account emails should not be printed in full. | Normal auth logs mask email addresses through `SafeLogValue`; reset and verification tokens stay hashed at rest. Demo mailbox preview links are only emitted when the explicit demo-mailbox mode is enabled. | Medium | Mitigated |
| Cache serialization | Redis serialization uses broad typing, which is convenient but should stay constrained. | Keep the polymorphic type validator tight or move to explicit serializers for the limited cached types. | Medium | Monitor |
| Actuator exposure | Health and metrics endpoints are useful, but public metrics can leak deployment details. | `health` and `info` stay public; `metrics` and `prometheus` require admin access by default, with an explicit local/dev exception for Prometheus scraping. | Medium | Mitigated |
| AI regeneration abuse | Public AI regeneration can consume backend or provider resources if anonymous traffic repeatedly triggers it. | AI metadata reads remain public, but regeneration now requires an authenticated user. If regeneration becomes expensive in demo/prod, add a dedicated AI rate-limit bucket or owner/admin authorization. | Medium | Mitigated |

## Notes

- Password hashes are stored in the database as encoded values, not plain text.
- The main remaining auth tradeoff is browser-side access-token storage. CSP and short token TTLs reduce the blast radius, but a BFF or cookie-only session model would be the next larger design step if the product needs stronger browser isolation.
- Refresh cookies default to `SameSite=Lax`. Do not switch demo/prod to cross-site cookies without adding an explicit CSRF token/header flow.
- SQL injection risk is currently low because the repository layer is mostly parameterized.
- Phase 20 hardening added security headers, stricter CORS validation, deployment-safe operational metrics, and endpoint-specific auth throttling.
- Phase 23 started a follow-up pass by centralizing public auth route constants across Spring Security and rate limiting, masking auth email addresses in logs, and protecting AI metadata regeneration behind authentication.
- Backend maintainability findings are tracked in [backend-code-quality-review.md](backend-code-quality-review.md).

## Phase 20 Security Defaults

### Browser security headers

The backend currently sends these headers through Spring Security:

| Header | Current value | Purpose |
| ------ | ------------- | ------- |
| `Content-Security-Policy` | `default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; img-src 'self' data: https:; font-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self' http://localhost:* http://127.0.0.1:* https:` | Limits script, image, font, style, frame, object, and API connection surfaces. |
| `Referrer-Policy` | `same-origin` | Keeps full referrer data only for same-origin navigation. |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=()` | Disables browser capabilities the app does not use. |
| `X-Frame-Options` | `SAMEORIGIN` | Legacy clickjacking protection in addition to CSP `frame-ancestors`. |

### Observability access

`APP_SECURITY_PUBLIC_OBSERVABILITY` decides whether actuator metrics endpoints are public.
It exists because local Prometheus needs unauthenticated scrape access, while demo/prod should not expose runtime metrics to the public internet.

| Profile / runtime | Default | Behavior |
| ----------------- | ------- | -------- |
| `local` | `true` | `/actuator/metrics` and `/actuator/prometheus` are public for local inspection and local Prometheus. |
| `dev` | `true` | `/actuator/metrics` and `/actuator/prometheus` are public so the Docker monitoring stack can scrape the backend. |
| `demo` | `false` | Only `/actuator/health` and `/actuator/info` are public; metrics and Prometheus require admin access. |
| `test` | `false` | Tests verify that metrics and Prometheus are protected by default. |
| default | `false` | Safe baseline for any runtime that does not explicitly opt in. |

This setting does not affect `/api/v1/admin/**`; admin APIs still require the admin role.
