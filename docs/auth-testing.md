# Auth Testing Workflow

This is the local end-to-end workflow for verifying the access-token plus refresh-token flow.

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

The frontend stores auth state in local storage under:

- `weblinkpilot.frontend.settings`

That object contains:

- `authToken`
- `refreshToken`

## 1. Sign In

Log in from the frontend or call the API directly.

API example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
```

Expected result:

- response includes both `token` and `refreshToken`
- the frontend saves both tokens in local storage
- the current user becomes authenticated in the UI

## 2. Verify Access Token

Use the access token against a protected endpoint such as `GET /api/v1/auth/me`.

API example:

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

## 3. Verify Refresh Bootstrap

This checks the app bootstrapping path.

1. Open browser devtools.
2. Go to Application or Storage.
3. Inspect `weblinkpilot.frontend.settings`.
4. Remove `authToken` but keep `refreshToken`.
5. Reload the frontend.

Expected result:

- the app calls `POST /api/v1/auth/refresh`
- it receives a rotated `token` and `refreshToken`
- the user stays signed in without logging in again

## 4. Verify Auto-Refresh On 401

This checks the request retry path.

1. Keep a valid `refreshToken`.
2. Replace `authToken` with an invalid value such as `junk`.
3. Trigger a protected request by refreshing the app or opening a protected page.

Expected result:

- the first protected request returns `401`
- the frontend calls `POST /api/v1/auth/refresh`
- the app retries the original request
- the page continues to work without manual re-login

## 5. Verify Logout

Sign out from the UI or call the logout endpoint directly.

API example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/logout" `
  -ContentType "application/json" `
  -Body '{"refreshToken":"<refresh-token-from-local-storage>"}'
```

Expected result:

- the frontend clears `authToken` and `refreshToken`
- the UI returns to guest mode
- the refresh token cannot be used again

## 6. Verify Refresh Rotation

This checks that the refresh token changes on every refresh.

1. Log in.
2. Save the current `refreshToken` value.
3. Trigger a refresh bootstrap or call `POST /api/v1/auth/refresh`.
4. Compare the returned refresh token with the old one.

Expected result:

- the new refresh token is different
- the old refresh token no longer works

## 7. Verify Rejection Paths

Use these to make sure invalid sessions fail cleanly:

- call `POST /api/v1/auth/refresh` with a blank token
- call it with an already revoked token
- call it after logout

Expected result:

- `401 Unauthorized`
- the response indicates an invalid refresh token

## 8. Optional Automated Checks

Run the focused backend auth tests from `backend/`:

```powershell
.\mvnw.cmd -Pci -pl app -am clean "-Dtest=AuthServiceTest,RefreshTokenServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Run the focused frontend auth tests from `frontend/`:

```powershell
npm run test:run -- src/lib/auth.test.ts src/lib/api.test.ts
```

## Quick Checklist

For a fast manual verification, do these four things:

1. log in
2. confirm both tokens are stored
3. remove `authToken` and reload to confirm refresh works
4. log out and confirm the refresh token is revoked

That covers the main access/refresh-token behavior end to end.
