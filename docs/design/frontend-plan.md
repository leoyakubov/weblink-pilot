# Frontend Plan

## Purpose

The frontend will be a Vue-based, mobile-first interface for the WeblinkPilot product.

Its job is to make link creation, QR handling, and analytics easy on both mobile and desktop without making the user think about backend complexity.

## Frontend goals

- mobile-first UX
- fast link creation flow
- clear short-link result screen
- QR code preview and download
- analytics dashboard with readable metrics
- responsive behavior for desktop and mobile
- API-driven architecture

## Recommended stack

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Tailwind CSS
- Vitest
- Playwright

## Page map

### 1. Landing / Create Link

Primary purpose:

- create a new short URL quickly

Main elements:

- original URL input
- custom alias input
- expiration selector
- create button
- optional advanced section

Mobile behavior:

- full-width form
- large touch targets
- sticky create action where useful

### 2. Link Created Success Screen

Primary purpose:

- confirm creation and provide share actions

Main elements:

- short link display
- copy button
- open button
- QR preview
- QR download action
- expiration summary

### 3. Redirect / Scan Experience

Primary purpose:

- act as the visual bridge when scanning a QR code or opening a short URL page

Notes:

- actual redirect happens in backend
- frontend can provide an optional preview or branded interstitial later

### 4. Analytics Dashboard

Primary purpose:

- show link performance and usage patterns

Main elements:

- total clicks
- unique visitors
- click trend chart
- recent clicks table
- browser/device/referrer breakdown
- country or geo summary

### 5. Link List / History

Primary purpose:

- manage previously created links

Main elements:

- search
- filters
- list rows or cards
- status indicators
- quick actions

### 6. Link Details

Primary purpose:

- show one link with stats and management actions

Main elements:

- original URL
- short URL
- QR code
- analytics snapshot
- edit/disable/delete actions later

### 7. Auth / Admin

Primary purpose:

- protect management views if we decide to expose them in the first version

Potential variants:

- simple login form
- token-based login later
- hidden behind basic auth for a first pass

## Component plan

### Core components

- `AppShell`
- `TopNav`
- `MobileBottomBar`
- `PageHeader`
- `LinkForm`
- `ExpirationPicker`
- `AliasInput`
- `ShortUrlCard`
- `QrPreviewCard`
- `CopyButton`
- `MetricCard`
- `AnalyticsChart`
- `RecentClicksTable`
- `EmptyState`
- `ErrorState`
- `LoadingState`

### Shared UI patterns

- card-based layout
- strong spacing hierarchy
- one primary action per screen
- bottom sheet or modal only when needed
- consistent status badges

## State management plan

Use Pinia for:

- created link state
- current link detail state
- analytics snapshot state
- UI preferences
- auth state if needed

Keep server state separated from local UI state.

Recommended split:

- server state through composables or lightweight query abstraction
- UI state in Pinia

## API integration plan

Frontend should consume backend-only APIs:

- create short link
- fetch short link details
- fetch QR code
- fetch analytics summary
- fetch link history

The frontend should not contain domain logic beyond presentation rules.

## QR code UX plan

QR should be visible in two places:

- success screen after creation
- link details screen

UX behavior:

- preview on screen
- download action
- copy short link action
- share action on mobile where supported

## Mobile-first layout plan

Layout principles:

- form-first on creation screens
- single-column default
- bottom-aligned primary CTA on small screens
- charts collapse into cards on mobile
- tables become stacked rows or condensed list items

## Design system direction

The design system should be simple but polished:

- strong typography hierarchy
- subtle gradients or accent backgrounds
- compact but readable cards
- visible feedback for copy, success, and loading states

## Routing plan

Suggested routes:

- `/` - landing and create form
- `/link/:code` - link details
- `/dashboard` - analytics dashboard
- `/history` - link history
- `/auth` - login or admin entry

## Testing plan

### Unit tests

- component rendering
- form validation
- computed UI states
- router guards

### Integration tests

- create link flow
- QR preview rendering
- dashboard data loading

### E2E tests

- create link and copy short URL
- create link and open QR
- navigate to dashboard and verify analytics widgets

## Future frontend evolution

Potential future additions:

- branded domains UI
- bulk short URL creation
- custom dashboard widgets
- theme switcher
- export analytics

