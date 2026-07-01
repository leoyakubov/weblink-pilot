# API Contract v1

## Principles

- versioned under `/api/v1`
- JSON-first
- stable error envelope
- frontend consumes only public contracts
- redirect endpoints remain lightweight

## Authentication

For the first version:

- JWT access tokens are used for API requests
- refresh tokens are stored in an `HttpOnly` cookie and rotate access sessions
- the API returns the access token, username, and role in JSON
- the refresh token itself is never returned in JSON
- management endpoints are protected
- public create/read endpoints may be open or protected depending on the final product flow

Refresh token endpoints:

- `POST /api/v1/auth/register` sets the refresh cookie on success
- `POST /api/v1/auth/login` sets the refresh cookie on success
- `POST /api/v1/auth/refresh` reads the refresh cookie, rotates it, and sets a new cookie
- `POST /api/v1/auth/logout` reads the refresh cookie and clears it

## Auth Endpoints

`POST /api/v1/auth/register`

`POST /api/v1/auth/login`

`POST /api/v1/auth/refresh`

`POST /api/v1/auth/logout`

`GET /api/v1/auth/me`

`GET /api/v1/auth/account`

`POST /api/v1/auth/account/password`

`POST /api/v1/auth/password-reset/request`

`POST /api/v1/auth/password-reset/confirm`

`POST /api/v1/auth/email-verification/request`

`POST /api/v1/auth/email-verification/confirm`

GitHub OAuth endpoints:

- `GET /api/v1/auth/oauth2/github/start`
- `GET /api/v1/auth/oauth2/github/callback`
- `POST /api/v1/auth/oauth2/github/complete`

## Endpoints

### 1. Create short URL

`POST /api/v1/urls`

Request:

```json
{
  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/some/long/link",
  "customAlias": "my-link",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Notes:

- `expiresAt` is optional
- if provided, it must not be in the past
- the backend caps it to the configured maximum lifetime from creation time

Response:

```json
{
  "code": "my-link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/some/long/link",
  "expiresAt": "2026-12-31T23:59:59Z",
  "createdAt": "2026-05-22T11:00:00Z",
  "qrCodeUrl": "https://weblink-pilot.io/api/v1/urls/my-link/qr",
  "aiMetadata": null
}
```

`aiMetadata` is `null` immediately after creation unless metadata already exists.
For list/read responses, it may contain the same object returned by `GET /api/v1/ai/links/{code}/metadata`.

### 2. Redirect

`GET /r/{code}`

Behavior:

- resolves short code
- returns HTTP redirect
- publishes click event asynchronously
- expired links return HTTP `410 Gone`
- expired link rows are archived after a grace period instead of being hard-deleted
- archived codes stay reserved and are not reused

### 3. Redirect preview

`GET /api/v1/urls/{code}/preview`

Response:

```json
{
  "code": "my-link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "targetUrl": "https://github.com/weblinkpilot/weblink-pilot/some/long/link",
  "status": 302,
  "locationHeader": "https://github.com/weblinkpilot/weblink-pilot/some/long/link"
}
```

Notes:

- meant for debugging and Swagger testing
- does not issue a real redirect

### 4. Get link details

`GET /api/v1/urls/{code}`

Response:

```json
{
  "code": "my-link",
  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/some/long/link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "createdAt": "2026-05-22T11:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "clickCount": 128,
  "status": "ACTIVE"
}
```

### 5. List links

`GET /api/v1/urls?page=0&size=10`

Compatibility:

- `limit=10` is still accepted and treated as `size=10` when `size` is not provided
- `page` is zero-based
- `size` is capped server-side by the configured browse maximum

Response:

```json
{
  "content": [
    {
      "code": "my-link",
      "shortUrl": "https://weblink-pilot.io/r/my-link",
      "qrCodeUrl": "https://weblink-pilot.io/api/v1/urls/my-link/qr",
      "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/some/long/link",
      "createdAt": "2026-05-22T11:00:00Z",
      "expiresAt": "2026-12-31T23:59:59Z",
      "clickCount": 128
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

Notes:

- returns the newest links first
- intended for authenticated dashboard and history views
- analytics overview uses this endpoint to page through visible links, then loads summaries for the current page

### 6. Get QR code

`GET /api/v1/urls/{code}/qr`

Response options:

- `image/png`

Suggested behavior:

- backend returns a generated QR code for the short URL
- frontend can render it inline or download it
- SVG support can be added later if we decide we want vector output

### 7. Get analytics summary

`GET /api/v1/analytics/{code}`

Response:

```json
{
  "code": "my-link",
  "totalClicks": 128,
  "uniqueVisitors": 91,
  "lastClickAt": "2026-05-22T11:30:00Z",
  "lastReferrer": "https://news.ycombinator.com",
  "lastBrowserFamily": "CHROME",
  "lastDeviceType": "MOBILE",
  "topCountries": [
    { "country": "US", "clicks": 84 },
    { "country": "DE", "clicks": 12 }
  ]
}
```

### 8. Get AI link metadata

`GET /api/v1/ai/links/{code}/metadata`

Response:

```json
{
  "code": "spring-boot",
  "status": "READY",
  "provider": "stub",
  "promptVersion": "link-metadata-v1",
  "title": "Spring Boot",
  "summary": "A clean short-link preview for spring.io that helps people understand the destination before opening it.",
  "category": "Programming",
  "tags": ["spring", "java", "backend"],
  "icon": "code",
  "suggestedAlias": "spring-boot",
  "errorMessage": null,
  "updatedAt": "2026-06-28T17:30:00Z",
  "completedAt": "2026-06-28T17:30:00Z"
}
```

Suggested behavior:

- link creation stays synchronous and fast
- AI enrichment runs asynchronously from `LinkCreatedEvent`
- `status` can be `PENDING`, `READY`, `FAILED`, or `DISABLED`
- the default `stub` provider does not call external AI services
- the UI does not display `suggestedAlias` yet; it is kept in the API for future create-flow assistance

### 9. Regenerate AI link metadata

`POST /api/v1/ai/links/{code}/metadata/regenerate`

Regenerates metadata for a link that already has an AI metadata row.
The response body is the updated `AiLinkMetadataResponse`.

### 10. Admin users

`GET /api/v1/admin/users?page=0&size=10`

Notes:

- admin-only endpoint
- `page` is zero-based
- `size` defaults to `10` and is capped server-side
- results are sorted by username ascending

Response:

```json
{
  "content": [
    {
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN",
      "enabled": true,
      "emailVerified": true,
      "createdAt": "2026-06-20T10:00:00Z",
      "lastLoginAt": "2026-06-21T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

### 11. Admin monitoring

`GET /api/v1/admin/monitoring`

Notes:

- admin-only endpoint
- returns health, runtime links, selected configuration, and service status for the monitoring page
- public actuator health/info stay separate from the admin monitoring API

## Error contract

Use a stable error payload:

```json
{
  "timestamp": "2026-05-22T11:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Original URL is required",
  "path": "/api/v1/urls"
}
```

## Error codes

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `ALIAS_ALREADY_EXISTS`
- `LINK_EXPIRED`
- `RATE_LIMIT_EXCEEDED`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `INTERNAL_ERROR`

## Frontend contract notes

The Vue app should rely on:

- `code`
- `shortUrl`
- `qrCodeUrl`
- `originalUrl`
- `createdAt`
- `expiresAt`
- `clickCount`
- analytics summary fields

The frontend should not need to know how code generation, persistence, or event processing work internally.

## Future additions

Later versions may add:

- `PATCH /api/v1/urls/{code}`
- `DELETE /api/v1/urls/{code}`
- bulk creation
- branded domains
- export endpoints
