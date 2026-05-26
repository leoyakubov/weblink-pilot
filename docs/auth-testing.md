# Auth Testing Workflow

This is the local end-to-end workflow for verifying the short-lived access-token plus refresh-cookie flow.

It covers:

- login and registration
- access-token use on protected API requests
- refresh-token bootstrap and rotation
- logout revocation
- browser storage behavior

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

## 3. Verify Refresh Bootstrap

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

## 4. Verify Auto-Refresh On 401

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

## 5. Verify Logout

Sign out from the UI or call the logout endpoint directly.

Expected result:

- the frontend clears `authToken`
- the browser clears the `weblinkpilot_refresh` cookie
- the UI returns to guest mode
- the refresh token cannot be used again

Browser checks:

- `weblinkpilot.frontend.session` no longer contains the previous access token
- the `weblinkpilot_refresh` cookie is removed or expires immediately

## 6. Verify Refresh Rotation

This checks that the refresh token changes on every refresh.

1. Log in.
2. Save the current `weblinkpilot_refresh` cookie value from devtools or the network tab.
3. Trigger a refresh bootstrap or call `POST /api/v1/auth/refresh`.
4. Compare the new cookie value with the old one.

Expected result:

- the new cookie value is different
- the old cookie value no longer works
- the browser session remains authenticated after the rotation

## 7. Verify Rejection Paths

Use these to make sure invalid sessions fail cleanly:

- call `POST /api/v1/auth/refresh` with a blank token
- call it with a missing cookie
- call it with an already revoked cookie
- call it after logout

Expected result:

- `401 Unauthorized`
- the response indicates an invalid refresh token
- no new access token is issued

## 8. Optional Automated Checks

Run the focused backend auth tests from `backend/`:

```powershell
.\mvnw.cmd -Pci -pl app -am clean "-Dtest=AuthServiceTest,AuthControllerTest,RefreshTokenServiceTest,JwtServiceTest,UserAccountServiceTest,AuthExceptionHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Run the focused frontend auth tests from `frontend/`:

```powershell
npm run test:run -- src/lib/auth.test.ts src/lib/api.test.ts
```

## Quick Checklist

For a fast manual verification, do these four things:

1. log in
2. confirm the access token is stored and the refresh cookie is set
3. remove `authToken` from session storage and reload to confirm refresh works from the cookie
4. log out and confirm the refresh cookie is cleared

That covers the main access-token and refresh-cookie behavior end to end.

## Suggested Local Order

If you want one clean smoke path after making auth changes, run these in order:

1. start the local stack
2. log in with `admin / admin123`
3. confirm access token in session storage
4. confirm `weblinkpilot_refresh` cookie in DevTools
5. open `GET /api/v1/auth/me`
6. delete the session token and refresh the page
7. force a `401` with a junk token and confirm auto-refresh
8. sign out and confirm the cookie is cleared

That sequence catches the most important auth regressions with the least manual effort.
