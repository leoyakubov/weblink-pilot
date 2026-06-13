# Auth Testing Workflow

This is the local end-to-end workflow for verifying the short-lived access-token plus refresh-cookie flow.

For the broader feature-by-feature testing matrix, see:

- [`feature-testing.md`](feature-testing.md)

It covers:

- login and registration
- access-token use on protected API requests
- email verification
- GitHub social login
- account profile loading
- password change
- refresh-token bootstrap and rotation
- logout revocation
- browser storage behavior
- remember-me session duration when the option is added

## Prerequisites

- Start the local Docker stack with the backend, frontend, Postgres, and Redis.
- Open the frontend at `http://localhost:8081`.
- Use the seeded demo accounts if you do not want to create a new user:
  - `admin / admin123`
  - `user / user123`

The frontend stores the browser-facing config in local storage under:

- `weblinkpilot.frontend.settings`

That object contains:

- `apiBaseUrl`

The short-lived access token lives in session storage under:

- `weblinkpilot.frontend.session`

The refresh token itself is stored in an `HttpOnly` cookie named:

- `weblinkpilot_refresh`

## Mail Setup

The app uses SMTP for password reset and email verification.
The backend also enforces a short resend cooldown for these links so duplicate clicks do not send multiple emails; repeated requests return `429 Too Many Requests` with a `Retry-After` header.

### Local

Use Mailpit in the local Docker stack. The `backend-local` launcher now starts the `mailpit` service automatically before the backend comes up:

- host: `localhost`
- port: `1025`
- username: empty
- password: empty
- SMTP auth: `false`
- STARTTLS: `false`

The Docker stack already wires these values through the backend container.

The local and dev backend profiles also perform a startup mail connection check. If Mailpit is not running, the backend should fail fast or log `mail.server.health status=DOWN` before you try to send any email.

If you want to run the backend on your machine instead of inside Docker, start only Mailpit and point the backend to it:

```powershell
docker compose -f infra/docker-compose.yml up -d mailpit
$env:SPRING_MAIL_HOST = "localhost"
$env:SPRING_MAIL_PORT = "1025"
```

```bash
docker compose -f infra/docker-compose.yml up -d mailpit
export SPRING_MAIL_HOST=localhost
export SPRING_MAIL_PORT=1025
```

Then start the backend with either the `local` profile or the `dev` profile. Both can work against Mailpit on `localhost:1025` when you override the SMTP host and port.

### Demo

Use Mailtrap Email Testing for the demo environment.

Recommended values:

```env
SPRING_MAIL_HOST=sandbox.smtp.mailtrap.io
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<your Mailtrap SMTP login>
SPRING_MAIL_PASSWORD=<your Mailtrap SMTP password>
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS=true
```

Mailtrap gives you a browser inbox where you can inspect the message body and click the verification/reset links in a separate tab without sending mail to real recipients. Use the SMTP credentials from the Mailtrap inbox settings, not an API token.

Useful references:

- [Mailtrap home](https://mailtrap.io/)
- [Mailtrap Email Testing](https://mailtrap.io/email-testing)

## 1. Sign In

Log in from the frontend or call the API directly.

API example:

```bash
curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

PowerShell example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
```

Expected result:

- response includes `token`
- the backend sets the `weblinkpilot_refresh` cookie
- the frontend saves the access token in session storage
- the current user becomes authenticated in the UI

Browser checks:

- open DevTools
- verify `weblinkpilot.frontend.session` contains the access token
- verify the `weblinkpilot_refresh` cookie is present and marked `HttpOnly`

## Remember Me (Planned)

This is the target behavior for the next auth usability step. The current app does not expose a dedicated remember-me toggle yet.

### What to test

- sign in with the remember-me option enabled
- sign in with the remember-me option disabled
- refresh-cookie lifetime changes only when the user opts in
- logout still clears the browser session and revokes refresh tokens

### Steps

1. Open the sign-in form once the remember-me control is available.
2. Sign in with remember-me enabled.
3. Close and reopen the browser or tab.
4. Confirm the session survives according to the longer refresh-cookie lifetime.
5. Sign out.
6. Sign in again without remember-me.
7. Confirm the default session remains shorter-lived for shared-device safety.

### Expected result

- the user can explicitly choose whether the session should be remembered
- the access-token model stays short-lived
- the refresh-cookie lifetime is longer only when remember-me is selected
- logout and refresh-token revocation keep working as expected

## GitHub Login

Use this flow when GitHub OAuth is configured in the environment.

### What to test

- GitHub popup start
- OAuth callback and ticket exchange
- existing-account reuse or new account creation
- refresh-cookie issuance after social sign-in

### Steps

1. Open the sign-in page.
2. Click `Continue with GitHub`.
3. Complete the GitHub authorization flow in the popup.
4. Wait for the popup to close or return to the completion page.
5. Check the main app window for the signed-in state.

### Expected result

- GitHub opens in a popup or tab
- the backend validates the OAuth callback and state cookie
- the popup completes through `/auth/github/complete`
- the frontend receives the login response and stores the access token
- the backend sets the refresh cookie
- the signed-in user appears in the app shell
- the social identity is reused on later GitHub logins

## Account Management

Use this flow to verify the signed-in account settings page.

### What to test

- account profile loading
- linked identities
- password change
- refresh-token revocation after password change

### Steps

1. Sign in with a local account or GitHub.
2. Open the account page.
3. Check the profile details and linked identities.
4. Change the password from the account page.
5. Refresh the browser and sign in again if needed.

### Expected result

- the account page loads the current profile
- linked social identities appear in the profile card
- changing the password succeeds with the current password
- the backend revokes existing refresh sessions after a password change
- the current browser session stays usable until the access token expires or refresh is used again

## 2. Verify Access Token

Use the access token against a protected endpoint such as `GET /api/v1/auth/me`.

API example:

```bash
curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token'

curl -s -X GET "http://localhost:8080/api/v1/auth/me" \
  -H "Authorization: Bearer <access-token>"
```

PowerShell example:

```powershell
$session = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/auth/me" `
  -Headers @{ Authorization = "Bearer $($session.token)" }
```

Expected result:

- `200 OK`
- user profile JSON with the signed-in username and role

If you want to prove the browser path instead of a raw API call:

- stay signed in
- open the monitoring page or any authenticated page
- confirm the request includes `Authorization: Bearer <token>`

## 3. Verify Email

After registering a new account, confirm the email verification link is sent.

Expected result:

- registration sends a verification email
- the email link points at `http://localhost:5173/auth/verify-email?token=...` when the frontend runs locally
- the email link points at `http://localhost:8081/auth/verify-email?token=...` in the Docker full stack
- confirming the link marks the account as verified
- the login screen can resend a verification email if needed

Manual flow:

1. register a new account with an email address
2. open Mailpit at `http://localhost:8025` for local testing, or open the Mailtrap inbox for demo and click the email links from the message preview
3. open the verification link
4. wait for the frontend confirmation page to say the email is verified
5. sign in again if you were testing the blocked-login flow

API examples:

```bash
curl -s -X POST "http://localhost:8080/api/v1/auth/email-verification/request" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com"}'
```

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/email-verification/request" `
  -ContentType "application/json" `
  -Body '{"email":"alice@example.com"}'
```

## 4. Verify Refresh Bootstrap

This checks the app bootstrapping path.

1. Open browser devtools.
2. Go to Application or Storage.
3. Inspect `weblinkpilot.frontend.session`.
4. Remove the stored access token but keep the refresh cookie in the browser.
5. Reload the frontend.

Expected result:

- the app calls `POST /api/v1/auth/refresh`
- it receives a new `token`
- the user stays signed in without logging in again
- the refresh cookie is rotated by the backend

## 5. Verify Auto-Refresh On 401

This checks the request retry path.

1. Keep a valid refresh cookie.
2. Replace the session token with an invalid value such as `junk`.
3. Trigger a protected request by refreshing the app or opening a protected page.

Expected result:

- the first protected request returns `401`
- the frontend calls `POST /api/v1/auth/refresh`
- the app retries the original request
- the page continues to work without manual re-login
- the old access token stops being used after the refresh

## 6. Verify Logout

Sign out from the UI or call the logout endpoint directly.

Expected result:

- the frontend clears `authToken`
- the browser clears the `weblinkpilot_refresh` cookie
- the UI returns to guest mode
- the refresh token cannot be used again

Browser checks:

- `weblinkpilot.frontend.session` no longer contains the previous access token
- the `weblinkpilot_refresh` cookie is removed or expires immediately

## 7. Verify Refresh Rotation

This checks that the refresh token changes on every refresh.

1. Log in.
2. Save the current `weblinkpilot_refresh` cookie value from devtools or the network tab.
3. Trigger a refresh bootstrap or call `POST /api/v1/auth/refresh`.
4. Compare the new cookie value with the old one.

Expected result:

- the new cookie value is different
- the old cookie value no longer works
- the browser session remains authenticated after the rotation

## 8. Verify Rejection Paths

Use these to make sure invalid sessions fail cleanly:

- call `POST /api/v1/auth/refresh` with a blank token
- call it with a missing cookie
- call it with an already revoked cookie
- call it after logout

Expected result:

- `401 Unauthorized`
- the response indicates an invalid refresh token
- no new access token is issued

## 9. Optional Automated Checks

Run the focused backend auth tests from `backend/`:

```powershell
.\mvnw.cmd -Pci -pl application -am clean "-Dtest=AuthServiceTest,AuthControllerTest,RefreshTokenServiceTest,JwtServiceTest,UserAccountServiceTest,AuthExceptionHandlerTest,PasswordResetServiceTest,EmailVerificationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Run the focused frontend auth tests from `frontend/`:

```powershell
npm run test:run -- src/lib/auth.test.ts src/lib/api.test.ts src/views/EmailVerificationRequestView.test.ts src/views/EmailVerificationConfirmView.test.ts
```

## Quick Checklist

For a fast manual verification, do these four things:

1. register a new account and confirm the verification email is sent
2. open the verification link and confirm the account becomes verified
3. log in and confirm the access token is stored and the refresh cookie is set
4. remove `authToken` from session storage and reload to confirm refresh works from the cookie
5. log out and confirm the refresh cookie is cleared

That covers the main access-token and refresh-cookie behavior end to end.

## Suggested Local Order

If you want one clean smoke path after making auth changes, run these in order:

1. start the local stack
2. register a new account with an email address
3. confirm the verification email arrives in Mailpit
4. open the verification link and confirm the account is verified
5. log in with `admin / admin123` or the new account
6. confirm access token in session storage
7. confirm `weblinkpilot_refresh` cookie in DevTools
8. open `GET /api/v1/auth/me`
9. delete the session token and refresh the page
10. force a `401` with a junk token and confirm auto-refresh
11. sign out and confirm the cookie is cleared

That sequence catches the most important auth regressions with the least manual effort.
