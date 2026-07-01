# Frontend Architecture Plan

## Purpose

The frontend is a Vue 3, mobile-first application for creating short links, sharing QR codes, browsing owned/demo links, inspecting analytics, managing account flows, and opening admin-only operational views.

The app should feel like a small real SaaS product while keeping the browser layer thin: presentation, routing, auth state, and HTTP calls live in the frontend; business rules stay in the backend.

## Current Stack

- Vue 3
- TypeScript
- Vite
- Vue Router
- PrimeVue controls and Nora theme preset
- Custom CSS tokens and page/component CSS files
- Vitest
- Playwright-based e2e smoke flows

The project does not use Tailwind or Pinia right now. Keep state simple with focused modules and composables unless the app grows enough to justify a dedicated state library.

## Folder Structure

```text
frontend/src/
|-- account/
|   |-- login/
|   |-- register/
|   |-- password-reset/
|   |-- email-verification/
|   |-- github/
|   `-- account-settings/
|-- admin/
|   |-- monitoring/
|   `-- users/
|-- core/
|-- features/
|   |-- about/
|   |-- analytics/
|   |-- home/
|   `-- links/
|-- router/
`-- shared/
    |-- components/
    |-- composables/
    |-- services/
    |-- types/
    `-- utils/
```

Rules:

- `router/` owns route registration and route groups.
- `core/` owns app shell, navigation, and layout chrome.
- `account/` owns auth, recovery, verification, OAuth completion, and account settings.
- `admin/` owns admin-only pages and operational utilities.
- `features/` owns product-facing pages.
- `shared/` owns reusable UI, HTTP/settings services, types, composables, and utilities.
- Page folders keep `.vue`, `.ts`, `.css`, and tests side by side.
- Feature-specific components stay beside the feature when they are not reused elsewhere.

## Page Map

| Route | Owner | Purpose |
| --- | --- | --- |
| `/` | `features/home` | Home page, create-link flow, latest links, QR modal, and pagination |
| `/links` | `features/links/history` | Links list with filters, quick actions, and pagination |
| `/history` | router redirect | Backward-compatible redirect to `/links` |
| `/link/:code` | `features/links/link` | Link details, QR code, copy/share/open actions, and JSON preview |
| `/analytics` | `features/analytics/analytics` | Analytics overview across visible links with pagination |
| `/dashboard` | router redirect | Backward-compatible redirect to `/analytics` |
| `/analytics/:code` | `features/analytics/analytics-detail` | Per-link analytics detail page |
| `/about` | `features/about` | Product, access, stack, implementation, API, and project reference |
| `/auth/signin` | `account/login` | Sign-in form |
| `/auth/signup` | `account/register` | Registration form |
| `/auth/forgot-password` | `account/password-reset/request` | Password reset request |
| `/auth/reset-password` | `account/password-reset/confirm` | Password reset confirmation |
| `/auth/verify-email/request` | `account/email-verification/request` | Verification email request |
| `/auth/verify-email` | `account/email-verification/confirm` | Verification confirmation |
| `/auth/github/complete` | `account/github` | GitHub OAuth ticket completion |
| `/account` | `account/account-settings` | Profile, password/security, and identity providers |
| `/account/security` | router redirect | Backward-compatible redirect to `/account` |
| `/monitoring` | `admin/monitoring` | Admin monitoring and runtime links |
| `/settings/reset` | `admin/monitoring/reset` | Browser settings reset utility |
| `/admin/users` | `admin/users` | Admin read-only users directory with pagination |

## Shared Components

Use shared components when a pattern appears on more than one page:

- `PageIntro` for page header text.
- `PanelCard` for standard content sections.
- `LinkList` for home/latest and links/history list rows.
- `LinkFilters` for links filtering controls.
- `PaginationControls` for page-based navigation.
- `QrCodeModal` for QR previews.
- `RefreshButton` for refresh actions.
- `AuthNoticeModal` for sign-in prompts.

Keep page-specific components close to the page. For example, Home-only helpers live in `features/home/components`.

## State And API Integration

Current state model:

- `account/AuthSession.ts` owns auth bootstrap, current user, access token state, refresh behavior, and logout.
- `account/AccountProfileState.ts` owns account profile state.
- API clients live beside the feature they serve, for example `LinksApi.ts`, `AnalyticsApi.ts`, `AccountApi.ts`, and `AdminApi.ts`.
- `shared/services/http.ts` is the common HTTP layer.
- `shared/services/settings.ts` owns browser-stored frontend settings such as API base URL.

Guidelines:

- Keep server state close to the page or feature that loads it.
- Do not put domain rules in the frontend.
- Use backend pagination metadata instead of calculating page counts in the browser.
- Keep tokens and sensitive auth behavior centralized in the account layer.

## Styling Direction

The app uses a custom CSS visual system, not a template migration.

Current conventions:

- Global `styles.css` keeps design tokens, resets, PrimeVue baseline wiring, and generic app-wide helpers.
- Page and component styles live beside their `.vue` files.
- Shared component styles live beside the shared component.
- Avoid moving page-specific selectors back into global CSS.
- Keep dark shell contrast and action visibility strong.

See [frontend-visual-system.md](frontend-visual-system.md) for visual decisions and anti-goals.

## Routing And Access

Vue Router is centralized under `src/router`.

Route guards:

- bootstrap auth state before route decisions
- redirect non-admin users away from admin routes
- redirect anonymous users away from authenticated account routes

Public pages include home, links, link details, analytics reads, auth/recovery pages, about, and browser reset.

Protected pages include account settings and admin-only monitoring/users pages.

## Testing Plan

Use nearby tests for page and feature behavior:

- `*.test.ts` beside page/controller files
- router tests under `src/router`
- API client tests beside the API client
- shared service/component tests beside the shared unit

Verification commands:

- `npm --prefix frontend run test:run`
- `npm --prefix frontend run lint`
- `npm --prefix frontend run build`
- `npm --prefix frontend run test:e2e`
- `bash ./scripts/run-before-push.sh fe` for the frontend quality gate

## Future Frontend Evolution

Potential future additions:

- branded domains UI
- bulk short URL creation
- API key management UI
- richer analytics export
- team or organization account screens
- SVG QR downloads if the backend adds vector output
