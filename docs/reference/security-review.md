# Security Review

This document tracks the main OWASP-style risks in the current codebase and gives each item a simple status so we can follow up on it over time.

Status meanings:
- `Mitigated` means the current code already addresses the issue reasonably well.
- `Open` means we should still plan a fix.
- `Monitor` means the current shape is acceptable for now, but we should keep reviewing it as the app grows.

| Area | Problem | Possible Solution | Priority | Status |
| --- | --- | --- | --- | --- |
| Password storage | Passwords are hashed before saving, which is correct. The main risk is accidentally bypassing the encoder in future flows. | Keep using `PasswordEncoder` everywhere a password is stored or changed, and keep tests around register, reset, and password change flows. | High | Mitigated |
| CSRF | Cookie-based refresh and logout endpoints are sensitive to CSRF when the frontend and backend are cross-site, especially with `SameSite=None` in demo. | Prefer same-site deployment, use the narrowest safe cookie policy, or add explicit CSRF protection for cookie-authenticated state-changing endpoints. | High | Open |
| XSS and token theft | Access tokens still live in browser storage, so any XSS bug could steal them. | Avoid `v-html` and raw HTML injection, keep access tokens short-lived, add CSP in production, and consider moving more auth state into cookies or a BFF later. | High | Open |
| SQL injection | Current JPA and JPQL queries are parameterized, so the risk is low as long as we avoid string-built SQL. | Keep using bound parameters and repository methods; avoid concatenating user input into SQL or native queries. | Medium | Mitigated |
| Broken access control | Public endpoints like metrics/prometheus and any newly added route can widen exposure if not reviewed. | Keep deny-by-default security, review every new route, and restrict observability endpoints in demo/prod. | High | Monitor |
| CORS misconfiguration | Loose allowed-origin patterns could let unwanted origins call authenticated APIs. | Keep origin allowlists strict per environment and avoid wildcard origins in demo/prod. | High | Open |
| Brute force and abuse | Login, refresh, password reset, and verification endpoints can be spammed or brute-forced. | Add endpoint-specific rate limits, backoff, and better audit logging for repeated auth failures. | Medium | Open |
| Sensitive logging | Reset and verification links must never be logged in full because they contain secret tokens. | Log the event, not the secret URL; keep reset and verification tokens hashed at rest. | Medium | Mitigated |
| Cache serialization | Redis serialization uses broad typing, which is convenient but should stay constrained. | Keep the polymorphic type validator tight or move to explicit serializers for the limited cached types. | Medium | Monitor |
| Actuator exposure | Health and metrics endpoints are useful, but public metrics can leak deployment details. | Keep `health` public if needed, but gate `metrics` and `prometheus` behind auth, network controls, or a separate management port. | Medium | Monitor |

## Notes

- Password hashes are stored in the database as encoded values, not plain text.
- The main remaining risk in the current auth design is not password storage; it is browser-side token exposure and cookie-based cross-site request handling.
- SQL injection risk is currently low because the repository layer is mostly parameterized.
