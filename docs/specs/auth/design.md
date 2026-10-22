# B1 — Authentication Design

**Project:** AI Code Quality Auditor  
**Status:** Draft — aligned with B1 requirements; pending review  
**Source:** Updated Project Bible, Revision 3

## 1. Architecture

The authentication feature uses Spring Boot, Spring Security, PostgreSQL, and Flyway.

The API validates incoming requests, delegates business logic to services, and uses repositories to access persistent data. Controllers must not expose database entities directly as API responses.

Authentication uses:
- Short-lived JWT access tokens returned in JSON.
- Rotating, opaque refresh tokens delivered through an HttpOnly cookie.
- Persistent refresh-token hashes for revocation and reuse detection.
- Stateless Bearer-token authentication for protected API requests.

## 2. Component Responsibilities

- **AuthController:** Exposes registration, login, refresh, and logout endpoints. Sets and clears the refresh cookie where appropriate.
- **UserController:** Exposes the current-user endpoint.
- **AuthService:** Coordinates registration, credential verification, rate limiting, and authentication flows.
- **UserService:** Handles user-related business logic and profile retrieval.
- **TokenService:** Creates and validates access tokens and manages refresh-token generation and rotation.
- **Security configuration:** Defines public and protected endpoints, stateless request authentication, CORS, and security error handling.
- **Repositories:** Access users, refresh tokens, and audit records in PostgreSQL.
- **DTOs:** Define validated request and response payloads.
- **Exception handling:** Converts application errors into consistent `application/problem+json` responses.

These are proposed responsibilities; final class names and package placement must be checked against the existing codebase.

## 3. Registration Flow

1. Receive email, password, and display name.
2. Validate the request and normalize the email by trimming whitespace and converting it to lowercase.
3. Hash the password with BCrypt.
4. Save the user with the default `USER` role.
5. Enforce email uniqueness through a database constraint to handle concurrent registration attempts.
6. Return HTTP 201 with the approved public user representation.

Duplicate normalized email addresses return HTTP 409. The response must not expose database entities, password hashes, or internal exception details.

Public registration must never grant privileged roles.

## 4. Login Flow

1. Receive email and password.
2. Normalize the email and apply login rate limiting.
3. Find the user and verify the password against the BCrypt hash.
4. Return the same generic HTTP 401 problem response for unknown users and incorrect passwords.
5. Create a signed JWT access token with a 15-minute lifetime.
6. Generate a cryptographically secure opaque refresh token.
7. Persist the refresh-token hash and associated lifecycle metadata.
8. Return the access token, expiry, and safe user profile in JSON.
9. Set the refresh token in the `refresh_token` HttpOnly cookie.

The refresh token must never appear in a response body or logs. Authentication responses must not be cached.

## 5. Access Token Validation

Protected requests carry an access token in the `Authorization` header using the Bearer scheme.

Spring Security validates the token's signature, expiry, issuer and required claims before granting access. The token must be rejected if expired, malformed, tampered with, or otherwise invalid.

**Initial token configuration:**
- Algorithm: HS256.
- Lifetime: 15 minutes.
- Signing secret: supplied through deployment configuration, never committed to source control.
- Secret strength: at least 256 bits of cryptographically random key material.
- Claims: subject containing the user ID, role, issued-at time, expiry, and issuer.

The role claim can become stale after a role change until the access token expires. If immediate role-change enforcement is required, the implementation must additionally consult authoritative role data or use an explicit invalidation strategy. The initial design accepts the short-lived-token window unless the Project Bible requires stronger behavior.

## 6. Refresh Cookie and CSRF Protection

The refresh cookie uses the following settings:

- Name: `refresh_token`
- `HttpOnly`: enabled
- `Secure`: enabled in production
- `SameSite`: `Strict`
- `Path`: `/api/v1/auth`
- Maximum age: seven days

Refresh and logout requests must:
1. Read the refresh token from the cookie, not the request body.
2. Require the agreed custom CSRF header.
3. Validate the request origin against the configured frontend origin allowlist.
4. Reject invalid or missing CSRF headers and invalid origins.

CORS must use explicit allowed origins. Wildcard origins must never be combined with credentialed requests.

The frontend keeps access tokens in memory and sends cookie-authenticated requests using `credentials: 'include'`. Refresh tokens must not be stored in browser `localStorage`.

During development, use the Vite proxy for `/api` requests. The production deployment must preserve the intended same-site cookie security model.

## 7. Refresh Token Lifecycle and Concurrency

Each refresh token is generated using a cryptographically secure random source. Only its hash is persisted.

The record must support:
- Token hash and unique constraint.
- Associated user.
- Token-family identifier.
- Creation and expiry timestamps.
- Used and revoked state.
- Replacement-token reference, where appropriate.

A successful refresh:
1. Validates the presented token and its lifecycle state.
2. Atomically claims the current token for rotation.
3. Creates and persists the replacement token hash.
4. Marks the previous token as used and links it to the replacement.
5. Returns a new access token and sets the replacement refresh cookie.

The database transaction must prevent two concurrent requests from successfully rotating the same token. Use a conditional update or equivalent locking strategy and check the affected-row count.

### Reuse detection

If a previously rotated token is presented again, revoke the associated token family. This is the initial security policy; the exact persistence model and transaction behavior must be verified against the approved database design.

A concurrent refresh request may be indistinguishable from token theft. Under this policy, a losing concurrent request can trigger family revocation, potentially invalidating the token issued by the winning request. This trade-off must be documented and tested rather than silently ignored.

Expired, revoked, unknown, or reused tokens must not produce a successful refresh response.

## 8. Logout Flow

1. Read the refresh token from the cookie.
2. Require the custom CSRF header and validate the request origin.
3. Revoke the presented refresh token when applicable.
4. Clear the refresh cookie using the same path and compatible attributes used when setting it.
5. Return HTTP 204 with no response body.

Logout must not treat the mere presence of a cookie as authentication. The endpoint must not expose token validity details unnecessarily.

## 9. Rate Limiting

The initial login rate limiter uses both:
- Client IP address.
- Normalized email address.

The initial implementation may use an in-memory limiter for the single-instance MVP. It must have configurable thresholds, return HTTP 429 when exceeded, and provide a `Retry-After` header where appropriate.

This design does not provide shared rate-limit state across multiple application instances. A distributed deployment must use shared storage or an equivalent coordinated limiter.

Unknown-user login attempts should perform a dummy password-hash verification to reduce obvious timing differences. Login responses must remain generic.

## 10. Database Dependencies and Auditing

The feature depends on persistent user records, refresh-token records, and audit records.

The final schema must be aligned with the approved database design proposal. Database constraints must enforce normalized email uniqueness and refresh-token hash uniqueness.

Required sensitive actions must be audited, particularly privileged role changes. Audit records must not contain raw credentials, raw tokens, or complete access tokens.

Never edit an already-applied Flyway migration. Introduce schema changes through new migrations after the schema has been reviewed and approved.

## 11. Security Error Handling

Application errors must use consistent `application/problem+json` responses following RFC 7807 conventions.

The implementation must handle:
- Invalid input.
- Duplicate registration.
- Invalid credentials.
- Invalid, expired, or revoked tokens.
- Missing or invalid CSRF headers.
- Invalid request origins.
- Unauthenticated and forbidden requests.
- Rate-limited login attempts.

Spring Security filter-chain errors, including authentication failures and access denials, must follow the same error-response conventions as controller-level errors.

Do not return stack traces, secret configuration, password data, or internal exception details.

## 12. Testing Strategy

### Registration and credentials
- Valid registration and default role.
- Input validation and email normalization.
- Duplicate registration, including concurrent attempts.
- BCrypt password-storage verification.
- No sensitive fields in response bodies.

### Login and access tokens
- Successful login and cookie issuance.
- Generic failure for unknown email and incorrect password.
- JWT signature, expiry, issuer, and claim validation.
- Cookie attributes and absence of refresh tokens in JSON.
- Rate-limit enforcement and `Retry-After` behavior.

### Refresh and logout
- Successful refresh and token rotation.
- Expired, revoked, unknown, and reused refresh-token rejection.
- Token-family revocation after reuse.
- Concurrent refresh attempts.
- Missing CSRF header and invalid origin rejection.
- Logout revocation and cookie clearing.
- No refresh token in response bodies or logs.

### Authorization and auditing
- Protected endpoints reject missing or invalid access tokens.
- Role-based authorization and ownership checks.
- Required audit record for privileged role changes.
- Consistent problem responses for security-filter and controller errors.

Use unit tests for isolated logic and integration tests against PostgreSQL for transaction, uniqueness, and token-lifecycle behavior.

## 13. Decisions and Limitations

The following decisions are established for the initial implementation:
- HS256 access tokens with a 15-minute lifetime.
- Seven-day opaque refresh tokens stored as hashes.
- HttpOnly refresh cookie with the agreed attributes.
- CSRF custom header and origin validation for refresh/logout.
- Token-family revocation on refresh-token reuse.
- Atomic token rotation.
- In-memory login rate limiting for the single-instance MVP.

The following must still be confirmed during implementation planning:
- Exact database columns, constraints, indexes, and foreign keys.
- Exact CSRF header name and configured allowed origins.
- Cookie behavior for local development and production deployment.
- Precise rate-limit thresholds and trusted-proxy handling for client IPs.
- Initial-administrator setup procedure.
- Whether role changes require immediate access-token invalidation.

These decisions must be resolved against the Project Bible, approved database proposal, and API contract before the affected implementation is finalized.

## 14. Definition of Done

B1 is complete when the approved requirements and API contract are implemented, database migrations are reviewed, security and integration tests pass, documentation is updated, and the changes are ready for code review.