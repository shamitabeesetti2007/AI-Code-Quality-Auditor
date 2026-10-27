# API Contract

**Project:** AI Code Quality Auditor
**Status:** Draft — B1 authentication contract; B2 endpoints pending
**Base path:** `/api/v1`

## 1. Conventions

- Content type: `application/json` for JSON requests and responses.
- JSON fields use `camelCase`.
- Validate requests at the API boundary.
- Never expose database entities directly.
- Authentication uses short-lived JWT Bearer access tokens.
- Refresh tokens are delivered only through an HttpOnly cookie.
- CSRF tokens are provisioned by `GET /api/v1/csrf` in a readable `XSRF-TOKEN`
  cookie and response body. Send the token in the `X-XSRF-TOKEN` header for
  state-changing requests protected by Spring Security.
- Errors use `application/problem+json` following RFC 7807 conventions.
- Authentication responses must not be cached.

## 2. Authentication

### 2.1 Register

**Request:** `POST /api/v1/auth/register`
**Access:** Public

Request body:

```json
{
  "email": "user@example.com",
  "password": "example-password",
  "displayName": "Example User"
}
```

Success: `201 Created`

Response body:

```json
{
  "id": "UUID",
  "email": "user@example.com",
  "displayName": "Example User",
  "role": "USER"
}
```

Rules:
- Email is trimmed and converted to lowercase before storage.
- Password must contain at least 10 characters.
- New users receive the `USER` role.
- Duplicate normalized email returns `409 Conflict`.
- Passwords and password hashes are never returned.

### 2.2 Login

**Request:** `POST /api/v1/auth/login`
**Access:** Public

Request body:

```json
{
  "email": "user@example.com",
  "password": "example-password"
}
```

Success: `200 OK`

Response body:

```json
{
  "accessToken": "JWT",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "UUID",
    "email": "user@example.com",
    "displayName": "Example User",
    "role": "USER"
  }
}
```

The response also sets the `refresh_token` cookie. The refresh token must not appear in JSON.

Invalid credentials return a generic `401 Unauthorized` response regardless of whether the email exists. Excessive failed attempts return a generic `429 Too Many Requests` `application/problem+json` response with a `Retry-After` header containing the number of seconds to wait.

### 2.3 Refresh

**Request:** `POST /api/v1/auth/refresh`
**Access:** Refresh cookie, CSRF cookie/header, and allowed Origin required

- Read the refresh token from the `refresh_token` cookie.
- Require the provisioned CSRF cookie/header pair and validate the request
  origin.
- Rotate the refresh token atomically.
- Return a new access token and safe user profile.
- Set the replacement refresh cookie.

Success response: same JSON schema as login. The refresh token must not appear in JSON.

Expired, revoked, invalid, or reused refresh tokens must not succeed.

### 2.4 Logout

**Request:** `POST /api/v1/auth/logout`
**Access:** Refresh cookie, CSRF cookie/header, and allowed Origin required

- Validate the provisioned CSRF cookie/header pair and request origin.
- Revoke the submitted refresh token when applicable.
- Clear the refresh cookie.

Success: `204 No Content`, with no response body.

If refresh-token revocation fails, return a generic `500 Internal Server Error`
`application/problem+json` response and still clear the refresh cookie.

### 2.5 Current User

**Request:** `GET /api/v1/users/me`
**Access:** Valid Bearer access token required

Success: `200 OK`

Response body:

```json
{
  "id": "UUID",
  "email": "user@example.com",
  "displayName": "Example User",
  "role": "USER"
}
```

### 2.6 Change User Role

**Request:** `PATCH /api/v1/admin/users/{userId}/role`
**Access:** Authenticated `ADMIN` only
**CSRF:** Include the Spring Security CSRF token required for state-changing requests.

Request body:

```json
{
  "role": "RESEARCHER"
}
```

The role must be `USER`, `RESEARCHER`, or `ADMIN`. The API rejects no-op
changes, administrator self-demotion, and demotion of the last active
administrator. Successful changes return `200 OK` with the safe user profile
shown above. Every successful role change creates an immutable audit event in
the same database transaction as the user update.

Invalid requests return `400 Bad Request`; an unauthenticated caller receives
`401 Unauthorized`; a non-administrator or administrator attempting to
self-demote receives `403 Forbidden`; a missing user receives `404 Not Found`;
and a no-op change or attempt to demote the last active administrator receives
`409 Conflict`.

## 3. Refresh Cookie

Before a CSRF-protected state-changing request, call `GET /api/v1/csrf`.
It returns `{"token":"...","headerName":"X-XSRF-TOKEN"}` and sets the
readable `XSRF-TOKEN` cookie. Send the returned token in the named header and
include the cookie with the request. The endpoint response is not cacheable.

The refresh cookie uses these settings:

- Name: `refresh_token`
- `HttpOnly`: true
- `Secure`: true in production
- `SameSite`: `Strict`
- `Path`: `/api/v1/auth`
- `Max-Age`: seven days

The browser must not expose the refresh token to frontend JavaScript. The frontend holds the access token in memory and uses `credentials: 'include'` for cookie-based requests.

Refresh, logout, and administrator role changes require the CSRF cookie/header
pair. Refresh and logout additionally require a valid request origin.

## 4. Error Response

Errors use `application/problem+json`.

Example:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid email or password."
}
```

Expected status codes include:

| Status | Meaning |
|---|---|
| `400` | Invalid request or validation failure |
| `401` | Missing or invalid authentication |
| `403` | Authenticated but not authorized, or applicable security rejection |
| `404` | Resource not found |
| `409` | Duplicate registration or conflicting state |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error |

Responses must not expose stack traces, password hashes, raw refresh tokens, or sensitive internal details.

## 5. CORS and Browser Requests

- Use an explicit allowed-origin list.
- Configure the backend allowlist as comma-separated exact origins in
  `APP_ALLOWED_ORIGINS` (bound to `app.security.allowed-origins`).
- Never combine wildcard origins with credentialed requests.
- Credentialed API CORS requests allow only `GET`, `POST`, and `PATCH`, with
  the `Authorization`, `Content-Type`, and `X-XSRF-TOKEN` request headers.
- Configure the frontend to use `credentials: 'include'` for cookie-based requests.
- During development, use the Vite proxy for `/api` requests.

## 6. Health

`GET /actuator/health` remains an operational endpoint and is not a substitute for business API health checks.

## 7. Code Analysis and Submissions

B2 submission and analysis endpoints remain pending specification. Their request schemas, response schemas, validation rules, ownership rules, and error cases must be agreed before implementation.

## 8. Contract Change Process

1. Update this document and `openapi.yaml`.
2. Review changes with the backend and frontend owners.
3. Update mocks and tests.
4. Implement against the agreed contract.
