# Feature Testing Guide

This is the feature-by-feature manual testing guide for WebLinkPilot.

Use it when you want to verify a specific user journey end to end, not just the backend or frontend in isolation.

## How To Read This

- `Feature` = the part of the app you want to verify
- `Steps` = the exact actions to perform
- `Expected result` = what must happen for the feature to be considered working
- `Local` = Docker stack on `localhost`
- `Demo` = deployed Netlify + Render environment

## Prerequisites

- Local stack running, or demo URLs available
- Frontend open in a browser
- For email flows:
  - Local: Mailpit at `http://localhost:8025`
  - Demo: real SMTP provider configured in the environment

## Quick Test Matrix

| Feature | Steps | Expected result |
|---|---|---|
| Authentication | Sign up, verify email, sign in, use GitHub login, refresh the page, sign out | Access token is issued, refresh cookie is set, social login works, bootstrap refresh works, logout clears session |
| Email workflow | Request password reset, confirm reset, request email verification, confirm verification | Reset and verification emails are sent, links work, the account state updates |
| Link creation | Create a short link with and without alias | Link is created, alias rules are enforced, expiration is accepted or rejected correctly |
| Redirect flow | Open the short URL and QR path | Redirect happens fast, click count increments, analytics source is recorded |
| Analytics | Open link analytics and summary pages | Click totals and breakdowns match what the link received |
| Admin monitoring | Open `/monitoring` as admin | Health, info, metrics, and local monitoring links are visible |

## 1. Authentication

### What to test

- registration
- login
- access-token storage
- refresh-cookie rotation
- logout
- blocked login for unverified accounts

### Steps

1. Open the sign-up page.
2. Register a new account with username, password, and email.
3. Open Mailpit locally or the demo inbox/SMTP flow in demo.
4. Click the email verification link.
5. Sign in with the new account.
6. Open DevTools and inspect storage/cookies.
7. Remove the access token and reload the page.
8. Sign out.

### Expected result

- registration succeeds
- verification email is sent
- the verification link marks the account as verified
- sign-in succeeds only after verification
- access token lives in session storage
- refresh token lives in an `HttpOnly` cookie
- reloading without the access token triggers refresh and keeps the user signed in
- logout clears the refresh cookie and the UI returns to guest mode

### Useful docs

- [`auth-testing.md`](auth-testing.md)

## 2. Email Workflow

### What to test

- password reset
- email verification
- SMTP delivery

### Steps

1. Request a password reset from the login page or reset page.
2. Open the delivered email in Mailpit locally or your SMTP inbox/provider flow in demo.
3. Click the reset link and set a new password.
4. Request an email verification link for a new account.
5. Open the verification email and click the link.

### Expected result

- password reset email is sent
- reset token works once and expires after the configured TTL
- password changes successfully
- verification email is sent
- verification token works once and marks the account as verified
- no secret links appear in application logs

## 3. Link Creation

### What to test

- random short-link generation
- custom aliases
- expiration validation
- guest vs signed-in ownership

### Steps

1. Open the home page.
2. Create a link with only the original URL.
3. Create a second link with a custom alias.
4. Create a link with a valid future expiration.
5. Try creating a link with an expiration that is too far in the future.
6. Try creating a link with an already used custom alias.
7. Try creating a link after signing in.

### Expected result

- the short link is generated automatically
- custom aliases are accepted when available
- invalid aliases are rejected
- expiration cannot exceed the configured maximum lifetime
- guest links remain anonymous
- signed-in links are owned by the current user

## 4. Redirect Flow

### What to test

- short URL redirect
- QR redirect path
- click counting
- source tracking

### Steps

1. Open a short URL in the browser.
2. Open the QR link or scan the QR code.
3. Refresh the analytics or detail view.

### Expected result

- the redirect lands on the target URL
- the redirect is fast
- click counts increase
- redirect clicks and QR scans are tracked as separate sources

## 5. Analytics

### What to test

- analytics summary
- recent click counts
- country/referrer/device data

### Steps

1. Create or pick a short link.
2. Generate a few clicks from redirect and QR paths.
3. Open the analytics page.
4. Check the summary cards and recent data.

### Expected result

- total clicks match the generated traffic
- redirect and QR counts are split correctly
- summary data reflects browser/device/referrer information
- admin analytics and link analytics both load without errors

## 6. Admin Monitoring

### What to test

- admin-only access
- health/info/metrics links
- local Prometheus and Grafana

### Steps

1. Sign in as an admin user.
2. Open `/monitoring`.
3. Click health, info, metrics, and Prometheus links.
4. In local Docker, open Prometheus and Grafana.

### Expected result

- non-admin users are redirected away from the page
- admin sees the monitoring cards
- health endpoint returns `UP`
- Prometheus and Grafana open locally in the Docker stack

## 7. Fast Local Smoke

For a quick sanity check after changes:

1. Start the local Docker stack.
2. Create a link.
3. Verify it redirects.
4. Register a new user and verify email.
5. Sign in and sign out.
6. Open analytics for the created link.

That catches the most common regressions without running the full test suite.

## 8. Automated Checks

These are the closest scripted checks for the same flows:

- Auth workflow: [`auth-testing.md`](auth-testing.md)
- Backend test strategy: [`backend-testing.md`](backend-testing.md)
- Deployment smoke: [`../operations/deployment.md`](../operations/deployment.md)
