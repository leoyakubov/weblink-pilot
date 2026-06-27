# Frontend Visual System

## Decision

WeblinkPilot no longer targets a Sakai template migration.

The current frontend direction is a custom, mobile-first product UI built with Vue 3, shared app components, custom CSS tokens, and PrimeVue controls where they are useful for standard form and action behavior.

PrimeVue remains a runtime dependency because the app uses its buttons, drawer, inputs, password fields, and theme preset. Sakai is not a dependency and should not be treated as the target visual system.

## Current Direction

The UI should feel like a small real SaaS product for personal link sharing:

- dark branded shell with visible top navigation and footer
- consistent page structure: page intro first, then reusable panels
- compact cards with clear hierarchy and bright readable text
- colorful feature cards for product capabilities
- mobile-first layouts that collapse cleanly
- visible action buttons on dark backgrounds
- consistent QR modal behavior across home, history, dashboard, and link detail flows

## Shared Components

Use these shared components before adding page-specific markup:

- `PageIntro` for page eyebrow, title, and description
- `PanelCard` for standard content panels
- `FeatureCard` for compact product/feature cards
- `RefreshButton` for refresh actions
- `LinkList` for latest/history link rows
- `AnalyticsSummaryPanel` for repeated analytics summaries

## Design Changes From The Current Redesign Pass

The latest frontend redesign pass changed the app from page-by-page custom panels toward shared structure:

- Home keeps the create-link form visible beside the fast-start content.
- Link feature cards are compact, colorful, and easier to scan.
- Latest/history links use a shared `LinkList` with alternating card backgrounds, visible frames, full short URLs, aliases, full URLs, share actions, and aligned expiry/click metadata.
- QR actions open an in-app modal consistently instead of opening a separate tab from history.
- Dashboard, Link Details, Monitoring, Profile, Security, About, History, and Home now follow the same page-intro-then-panels structure.
- Profile and Security are separated into dedicated routes.
- Profile details use compact label/value rows and human-readable dates.
- Monitoring owns operational settings such as backend base URL and browser reset.
- About is now read-only product and implementation context.
- The top navigation, footer, refresh buttons, sign-out button, monitoring buttons, tooltips, and link-card actions were made more visible on the dark background.

## What We Avoid

Do not reintroduce a Sakai migration track unless the roadmap is explicitly changed again.

Avoid:

- template-specific CSS modes such as `data-ui-mode="sakai"`
- visible legacy/template switches
- documenting Sakai as an adopted dependency
- duplicating panels and link rows per page when a shared component already exists

## Verification

For frontend visual changes, run:

```sh
bash ./scripts/run-before-push.sh fe
```

At minimum, user-facing UI changes should keep lint, formatting, Vitest, e2e, and production build green.
