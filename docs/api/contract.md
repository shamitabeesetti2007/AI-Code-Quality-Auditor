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

Invalid credentials return a generic `401 Unauthorized` response regardless of whether the email exists. Excessive failed attempts return `429 Too Many Requests`.

### 2.3 Refresh

**Request:** `POST /api/v1/auth/refresh`
**Access:** Refresh cookie and CSRF header required

- Read the refresh token from the `refresh_token` cookie.
- Require the agreed custom CSRF header and validate the request origin.
- Rotate the refresh token atomically.
- Return a new access token and safe user profile.
- Set the replacement refresh cookie.

Success response: same JSON schema as login. The refresh token must not appear in JSON.

Expired, revoked, invalid, or reused refresh tokens must not succeed.

### 2.4 Logout

**Request:** `POST /api/v1/auth/logout`
**Access:** Refresh cookie and CSRF header required

- Validate the request origin and CSRF header.
- Revoke the submitted refresh token when applicable.
- Clear the refresh cookie.

Success: `204 No Content`, with no response body.

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

## 3. Refresh Cookie

The refresh cookie uses these settings:

- Name: `refresh_token`
- `HttpOnly`: true
- `Secure`: true in production
- `SameSite`: `Strict`
- `Path`: `/api/v1/auth`
- `Max-Age`: seven days

The browser must not expose the refresh token to frontend JavaScript. The frontend holds the access token in memory and uses `credentials: 'include'` for cookie-based requests.

Refresh and logout require the agreed custom CSRF header and a valid request origin.

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
- Never combine wildcard origins with credentialed requests.
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
