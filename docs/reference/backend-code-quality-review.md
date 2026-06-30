# Backend Code Quality Review

This review tracks the Phase 23 backend pass against common maintainability smells and the practical Effective Java, 3rd edition, guidance that applies to this codebase.

## Fixed in the current pass

| Area | Change | Why it helps |
| --- | --- | --- |
| Security route policy | Centralized repeated API route strings in `ApiRoutes`. | Reduces duplicated security/rate-limit route definitions and keeps deny-by-default routing easier to review. |
| AI regenerate endpoint | Changed AI metadata regeneration from public to authenticated. | Public metadata reads are still useful, but regeneration is a write-like operation that can consume backend/provider resources. |
| Sensitive auth logging | Masked email addresses in auth reset, verification, mail, controller, and GitHub OAuth logs through `SafeLogValue`. | Keeps logs operationally useful without leaking full email addresses or account-action details. |
| Link filtering | Introduced `LinkSearchCriteria` and `ExpirationFilter`. | Replaces loose string flags with explicit values and a single criteria object. |
| Link response mapping | Introduced `LinkResponseMapper`. | Prevents DTO construction from being duplicated across creation and lookup services. |
| Demo seed data | Introduced `DemoSeedDataCatalog`. | Link seeding, AI metadata seed data, and monitoring health checks now share the same demo-link catalog. |
| Token hashing | Introduced `TokenDigest`. | Moves refresh-token hashing into a focused utility with a deterministic unit test. |
| Admin monitoring split | Split `AdminMonitoringService` into runtime metrics, health checks, configuration snapshot, and a small facade. | Removes a god service and makes operational sections testable and maintainable independently. |
| Refresh token support split | Extracted `RefreshTokenSessionCache`, `RefreshTokenUserIndex`, and `AfterCommitExecutor`. | Keeps token rotation focused on lifecycle rules while Redis and transaction mechanics live behind reusable collaborators. |
| AI provider support split | Extracted `AiMetadataPromptRenderer` and `AiMetadataJsonParser`. | Keeps OpenAI-compatible and Ollama providers focused on transport/request envelopes instead of duplicating prompt and result parsing logic. |
| URL creation split | Extracted `ShortLinkCreationValidator` and `ShortCodeAllocationStrategy`. | Keeps link creation orchestration readable while validation and generated-code allocation are tested through focused collaborators. |
| Monitoring health status enum | Introduced `AdminHealthStatus`. | Replaces loose health-status strings inside monitoring code while preserving the existing response values for the frontend. |

## Effective Java checklist

| Item family | Current status |
| --- | --- |
| Prefer immutability and small value types | Records are used for many API contracts; new filter criteria also uses a record. Keep entity classes mutable only where JPA requires it. |
| Minimize mutability and scope | New helper classes keep constants and normalization close to their domain. Monitoring, refresh-token support, AI provider helpers, and URL creation have been split. |
| Use enums instead of loose constants | `ExpirationFilter` and `AdminHealthStatus` replace strict string states for link expiration filtering and monitoring health. Owner-role filters remain a future candidate. |
| Prefer dependency injection over hidden construction | `LinkResponseMapper` is a Spring component; services no longer duplicate DTO construction internally. |
| Avoid unnecessary checked exceptions leaking into callers | Token digesting wraps impossible SHA-256 lookup failure as an illegal state, keeping callers clean. |
| Do not ignore security boundaries | AI regeneration now requires authentication. Remaining owner/admin authorization could be tightened if regeneration should be limited to link owners. |
| Design for extension through interfaces where variation is expected | Existing AI provider abstraction is good; refresh-token cache/index behavior is now behind focused collaborators. |

## Future backend refactor candidates

These are not blockers for Phase 23. They are useful follow-up ideas if the code around the area changes again.

| Priority | Candidate | Suggested direction |
| --- | --- | --- |
| Medium | Analytics seed event details still live inside `AnalyticsBootstrapService`. | Move rich analytics seed fixtures to an `analytics.bootstrap` package or YAML-backed seed properties. |
| Low | Some Spring services could be marked `final`, but transactional/proxy behavior should be checked first. | Start with utility classes and non-proxied helpers only; avoid broad mechanical changes. |
| Low | Some internal records are nested inside services. | Move them only when reused or when the service split makes a dedicated package clearer. |

## Endpoint security notes

- Public by design: redirects, QR redirects, public URL reads/previews/QR images, public analytics reads, auth start/confirm flows.
- Authenticated: account profile/password, current user, AI metadata regeneration.
- Admin-only: `/api/v1/admin/**`, and metrics/prometheus unless `APP_SECURITY_PUBLIC_OBSERVABILITY=true` for local/dev observability.
- Deny by default remains the baseline for routes not explicitly listed.
