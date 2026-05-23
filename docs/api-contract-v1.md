# API Contract v1

## Principles

- versioned under `/api/v1`
- JSON-first
- stable error envelope
- frontend consumes only public contracts
- redirect endpoints remain lightweight

## Authentication

For the first version:

- management endpoints are protected
- public create/read endpoints may be open or protected depending on the final product flow
- we can use basic auth initially and upgrade to token-based auth later

## Endpoints

### 1. Create short URL

`POST /api/v1/urls`

Request:

```json
{
  "originalUrl": "https://example.com/some/long/link",
  "customAlias": "my-link",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Response:

```json
{
  "code": "my-link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "originalUrl": "https://example.com/some/long/link",
  "expiresAt": "2026-12-31T23:59:59Z",
  "createdAt": "2026-05-22T11:00:00Z",
  "qrCodeUrl": "https://weblink-pilot.io/api/v1/urls/my-link/qr"
}
```

### 2. Redirect

`GET /r/{code}`

Behavior:

- resolves short code
- returns HTTP redirect
- publishes click event asynchronously

### 3. Redirect preview

`GET /api/v1/urls/{code}/preview`

Response:

```json
{
  "code": "my-link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "targetUrl": "https://example.com/some/long/link",
  "status": 302,
  "locationHeader": "https://example.com/some/long/link"
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
  "originalUrl": "https://example.com/some/long/link",
  "shortUrl": "https://weblink-pilot.io/r/my-link",
  "createdAt": "2026-05-22T11:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "clickCount": 128,
  "status": "ACTIVE"
}
```

### 5. List recent links

`GET /api/v1/urls?limit=10`

Response:

```json
[
  {
    "code": "my-link",
    "shortUrl": "https://weblink-pilot.io/r/my-link",
    "qrCodeUrl": "https://weblink-pilot.io/api/v1/urls/my-link/qr",
    "originalUrl": "https://example.com/some/long/link",
    "createdAt": "2026-05-22T11:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z",
    "clickCount": 128
  }
]
```

Notes:

- returns the newest links first
- intended for authenticated dashboard and history views
- `limit` is capped server-side

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
- `GET /api/v1/urls`
- auth token login endpoints
- bulk creation
- branded domains
- export endpoints
