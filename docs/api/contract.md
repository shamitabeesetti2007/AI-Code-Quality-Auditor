# API Contract

**Project:** AI Code Quality Auditor
**Status:** B1 authentication and B2 submission/analysis contract
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
- Submission and analysis resources are scoped to the authenticated user.
- Submission source code is returned only by the individual submission detail
  response, which uses `Cache-Control: no-store`.

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
| `413` | Request payload exceeds the allowed source size |
| `422` | Unsupported language, origin, or clearly non-Java source |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error |

Responses must not expose stack traces, password hashes, raw refresh tokens, or sensitive internal details.

## 5. CORS and Browser Requests

- Use an explicit allowed-origin list.
- Configure the backend allowlist as comma-separated exact origins in
  `APP_ALLOWED_ORIGINS` (bound to `app.security.allowed-origins`).
- Never combine wildcard origins with credentialed requests.
- Credentialed API CORS requests allow `GET`, `POST`, `PUT`, `PATCH`, and `DELETE`, with
  the `Authorization`, `Content-Type`, and `X-XSRF-TOKEN` request headers.
- Configure the frontend to use `credentials: 'include'` for cookie-based requests.
- During development, use the Vite proxy for `/api` requests.

## 6. Health

`GET /actuator/health` remains an operational endpoint and is not a substitute for business API health checks.

## 7. Code Analysis and Submissions

Submission and analysis routes require an authenticated user and the CSRF
cookie/header pair on state-changing requests. Resource owners are derived
only from the authenticated principal. A request for another user's resource
returns `404 Not Found`.

### 7.1 Create submission

**Request:** `POST /api/v1/submissions/`

```json
{
  "title": "Example",
  "language": "JAVA",
  "originType": "HUMAN",
  "sourceCode": "class Example {}",
  "autoAnalyze": true
}
```

`autoAnalyze` defaults to `true`. Supported `language` is currently `JAVA`.
Client origins are `HUMAN`, `AI_GENERATED`, and `AI_ASSISTED`;
`AI_REMEDIATED` is reserved for trusted internal workflows. User-declared AI
provenance is never marked verified by this API.

Titles are required and limited to 120 characters. Source is required,
nonblank, and limited to 100,000 UTF-8 bytes after a leading UTF-8 BOM is
removed and CRLF/CR line endings are normalized to LF. A SHA-256 hash is
calculated over that normalized content. Logical LOC excludes blank lines
and obvious comment-only lines; it is an approximation, not a syntax-aware
Java measurement. Clearly recognizable non-Java source and unsupported
languages return `422 Unprocessable Content`. Java syntax errors are not
rejected at submission time.

Success: `201 Created`. The response contains a source-free submission
summary and, when automatic analysis is enabled, the newly queued analysis
run. Source code is not echoed in this response.

### 7.2 List, read, update, and delete submissions

- `GET /api/v1/submissions?page=0&size=20` lists only the caller's submissions,
  newest first. `size` is limited to 1–100. The response is
  `{ "items": [], "page": 0, "size": 20, "totalItems": 0, "totalPages": 0 }`.
  List items never contain source code.
- `GET /api/v1/submissions/{submissionId}` returns the caller's submission,
  including source code. The response must have `Cache-Control: no-store`.
- `PUT /api/v1/submissions/{submissionId}` accepts `title` and `sourceCode`,
  recalculates normalized source hash and logical LOC, and returns a
  source-free summary.
- `DELETE /api/v1/submissions/{submissionId}` deletes the owned submission
  and its associated analysis runs, metrics, and findings; success is
  `204 No Content`.

### 7.3 Analysis runs

- `POST /api/v1/submissions/{submissionId}/analysis-runs` manually queues a
  run for an owned submission.
- `GET /api/v1/submissions/{submissionId}/analysis-runs` lists only runs for
  an owned submission, newest first.
- `GET /api/v1/analysis-runs/{runId}` returns an owned run with persisted
  metrics and findings.

New runs start in `QUEUED`. Supported lifecycle statuses are `QUEUED`,
`PARSING`, `ANALYZING`, `SCORING`, `COMPLETED`, `PARTIAL`, `PARSE_FAILED`,
and `FAILED`. `COMPLETED`, `PARTIAL`, `PARSE_FAILED`, and `FAILED` are terminal.
Only one active run may exist per submission. A request to create a second
active run returns `409 Conflict`. The current backend creates queued runs
only; analyzer execution is not yet connected.
Each newly created run captures its submission's normalized source and
SHA-256 hash. The snapshot is immutable and is not included in API responses;
the run summary includes `sourceHash`. Runs created before this change retain
`null` for the source snapshot and hash rather than being attributed to the
submission's current source.

Source-code responses use no-store caching. All errors use
`application/problem+json`; `413` indicates the source byte limit was
exceeded, `422` indicates an unsupported language, origin, or clearly
non-Java source, and `409` indicates an active analysis run conflict.

## 8. Contract Change Process

1. Update this document and `openapi.yaml`.
2. Review changes with the backend and frontend owners.
3. Update mocks and tests.
4. Implement against the agreed contract.
