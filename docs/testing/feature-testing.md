# Feature Testing Guide

This is the feature-by-feature manual testing guide for WeblinkPilot.

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
  - Demo: real SMTP through Brevo, with messages arriving in the tester's mailbox

## Quick Test Matrix

| Feature          | Steps                                                                                                                                | Expected result                                                                                                                                                                 |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Authentication   | Sign up, verify email, sign in, use GitHub login when configured, open account settings, change password, refresh the page, sign out | Access token is issued, refresh cookie is set, social login works when configured, account profile loads, password change works, bootstrap refresh works, logout clears session |
| Remember me      | Sign in with and without the remember-me option once it is added                                                                     | Trusted-device sessions last longer only when the user opts in, while the default sign-in remains short-lived                                                                   |
| Email workflow   | Request password reset, confirm reset, request email verification, confirm verification                                              | Reset and verification flows arrive in Mailpit locally or in the tester's inbox for demo, links work, the account state updates                                                 |
| Link creation    | Create a short link with and without alias                                                                                           | Link is created, alias rules are enforced, expiration is accepted or rejected correctly                                                                                         |
| Redirect flow    | Open the short URL and QR path                                                                                                       | Redirect happens fast, click count increments, analytics source is recorded                                                                                                     |
| Analytics        | Open `/analytics` and `/analytics/:code`                                                                                             | Click totals and breakdowns match what visible links received                                                                                                                   |
| Admin monitoring | Open `/monitoring` as admin                                                                                                          | Health checks, runtime metrics, configuration, service endpoints, and local monitoring links are visible                                                                        |
| Admin users      | Open `/admin/users` as admin                                                                                                         | Registered accounts, roles, status, and recent sign-in activity are visible                                                                                                     |

## 1. Authentication

### What to test

- registration
- login
- access-token storage
- refresh-cookie rotation
- logout
- blocked login for unverified accounts
- remember-me session duration

### Steps

1. Open the sign-up page.
2. Register a new account with username, password, and email.
3. Open Mailpit locally or the received email in your inbox for demo.
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
2. Open the delivered email in Mailpit locally or the received email in your inbox for demo.
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
- analytics overview and per-link analytics details

### Steps

1. Create or pick a short link.
2. Generate a few clicks from redirect and QR paths.
3. Open `/analytics`.
4. Filter the visible links if needed.
5. Open `/analytics/:code` for one link.
6. Check the summary cards, charts, breakdowns, and recent data.

### Expected result

- total clicks match the generated traffic
- redirect and QR counts are split correctly
- summary data reflects browser/device/referrer information
- analytics overview and link analytics details both load without errors

## 6. Admin Monitoring

### What to test

- admin-only access
- health checks
- runtime metrics
- configuration values
- service endpoint links
- local Prometheus and Grafana

### Steps

1. Sign in as an admin user.
2. Open `/monitoring`.
3. Check the health, runtime metrics, configuration, and service endpoint panels.
4. Open Swagger UI from the service endpoints panel.
5. In local Docker, open Prometheus and Grafana from the service endpoints panel.

### Expected result

- non-admin users are redirected away from the page
- admin sees the monitoring cards
- health checks use clear `UP`, warning/info, or error statuses
- runtime metrics and configuration load from the backend
- Prometheus and Grafana open locally in the Docker stack

## 7. Admin Users

### What to test

- admin-only access
- user list loading
- role and account status display
- recent sign-in timestamps

### Steps

1. Sign in as `admin / admin123`.
2. Open the user dropdown.
3. Click `Users`.
4. Review `/admin/users`.
5. Try opening `/admin/users` as a non-admin user.

### Expected result

- admin sees user stats and the user directory
- user rows show username, role, status, email, created time, and last login
- non-admin users are redirected away from the page

## 8. Fast Local Smoke

For a quick sanity check after changes:

1. Start the local Docker stack.
2. Create a link.
3. Verify it redirects.
4. Register a new user and verify email.
5. Sign in and sign out.
6. Open `/links`, `/link/:code`, `/analytics`, and `/analytics/:code`.
7. Sign in as admin and open `/monitoring` and `/admin/users`.

That catches the most common regressions without running the full test suite.

## Flow Checklist

These are the same journeys summarized in the README diagrams. Use this compact checklist when validating a build manually:

- Auth: register, verify email, sign in, refresh page, sign out.
- Email: request password reset, open the delivered email, set a new password, confirm old reset links cannot be reused.
- Links: create guest link, create signed-in link, open redirect, open QR, confirm ownership in `/links`.
- Analytics: generate redirect and QR traffic, open `/analytics`, open `/analytics/:code`, compare totals and recent events.
- Admin: sign in as `admin / admin123`, open `/monitoring`, inspect health/metrics/configuration/endpoints, open `/admin/users`.

## 9. Automated Checks

These are the closest scripted checks for the same flows:

- Auth workflow: [`auth-testing.md`](auth-testing.md)
- Backend test strategy: [`backend-testing.md`](backend-testing.md)
- Deployment smoke: [`../operations/deployment.md`](../operations/deployment.md)
