# B1 — Authentication & Security Requirements

**Project:** AI Code Quality Auditor  
**Status:** Draft — review before implementation  
**Source:** Updated Project Bible, Revision 3

## 1. Objective

Build secure user authentication and authorization using Spring Boot, Spring Security, PostgreSQL, JWT-based access tokens, and rotating refresh tokens delivered through an HttpOnly cookie.

## 2. Functional Requirements

### FR-01: Registration
- Accept email, password, and display name.
- Validate user input at the API boundary.
- Require a password of at least 10 characters.
- Return HTTP 409 if the normalized email is already registered.
- Normalize email by trimming whitespace and converting it to lowercase.
- Store a BCrypt password hash, never the plaintext password.
- Never allow public registration to assign privileged roles.

### FR-02: Login
- Authenticate using email and password.
- Issue a short-lived JWT access token with a 15-minute lifetime.
- Return the access token, its expiry, and the public user profile in the JSON response.
- Set the refresh token in an HttpOnly cookie; never return it in JSON.
- Return the same generic HTTP 401 response for an unknown email and an incorrect password.
- Rate-limit repeated login failures and return HTTP 429 when the limit is exceeded.
- Do not log passwords, access tokens, or refresh tokens.

### FR-03: Refresh Tokens
- Accept the refresh token only through the `refresh_token` cookie.
- Require the agreed custom CSRF header and validate the request origin.
- Issue a new access token and rotate the refresh token when a valid refresh token is presented.
- Store refresh-token hashes, not raw refresh tokens.
- Reject expired, revoked, or reused refresh tokens.
- Detect refresh-token reuse and revoke the token family according to the approved design.
- Set the replacement refresh token in the same cookie.
- Do not return refresh tokens in request or response bodies.

### FR-04: Logout
- Accept the refresh token through the `refresh_token` cookie.
- Require the agreed custom CSRF header and validate the request origin.
- Revoke the submitted refresh token when valid.
- Clear the refresh cookie using the same path and compatible attributes used when setting it.
- Return HTTP 204 on successful logout.
- Logout must not rely on the mere presence of a cookie as proof of authentication.

### FR-05: Current User
- Return the authenticated user's ID, email, display name, and role.
- Never return passwords, password hashes, or refresh-token data.
- Require a valid Bearer access token.

### FR-06: Roles and Authorization
- Support USER, RESEARCHER, and ADMIN roles.
- Protect endpoints that require authentication.
- Check resource ownership in the backend service layer.
- Return 404 for another user's protected resource where required by the Project Bible.
- Create the first administrator through a controlled setup process, never public registration.
- Ensure privileged role changes are authorized and audited.

### FR-07: Security Auditing
- Record required sensitive security actions.
- Ensure role changes create the required audit record.
- Never record raw passwords, raw refresh tokens, or complete access tokens in audit logs.

## 3. API Endpoints

All business endpoints use the `/api/v1` base path.

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Refresh cookie + CSRF header |
| POST | `/api/v1/auth/logout` | Refresh cookie + CSRF header |
| GET | `/api/v1/users/me` | Bearer access token |

The access token is returned in JSON and held in frontend memory. The refresh token is delivered only through the HttpOnly cookie.

Request schemas, response schemas, validation, cookie behavior, CSRF requirements, and error responses must be finalized in the API contract before implementation.

## 4. Cookie and Browser Security

The refresh cookie must use these settings:

- Name: `refresh_token`
- `HttpOnly`: enabled
- `Secure`: enabled in production
- `SameSite`: `Strict`
- `Path`: `/api/v1/auth`
- Maximum age: seven days

The frontend must not store refresh tokens in `localStorage` or expose them to JavaScript.

Refresh and logout must require a custom request header and validate the request origin. CORS must use an explicit origin allowlist; wildcard origins must not be combined with credentials.

During development, configure the Vite proxy to route `/api` requests to Spring Boot so browser requests remain same-origin.

The production deployment must preserve the same-site cookie security model. The deployment strategy must be agreed before production release.

## 5. Security Requirements

- Use BCrypt for password hashing.
- Use short-lived JWT access tokens and validate signature, expiry, and required claims.
- Keep JWT signing secrets outside source control and load them from configuration.
- Rotate refresh tokens and store their hashes.
- Reject expired or tampered access tokens.
- Detect refresh-token reuse and apply the approved revocation behavior.
- Apply login rate limiting.
- Enforce authorization on the backend.
- Use stateless request authentication as required by the architecture.
- Return consistent `application/problem+json` errors following RFC 7807 conventions.
- Prevent caching of sensitive authentication responses.
- Create the initial administrator using a controlled setup process.

## 6. Error Handling

Use consistent error responses with the `application/problem+json` media type, following the Project Bible's RFC 7807 requirement.

Handle invalid input, duplicate registration, invalid credentials, expired or revoked tokens, unauthorized requests, forbidden actions, CSRF failures, and rate-limited login attempts.

Login errors must not reveal whether an email address exists.

The API contract must specify status codes and error schemas before implementation.

## 7. Acceptance Criteria

- [ ] Valid registration succeeds.
- [ ] Duplicate registration returns HTTP 409.
- [ ] Passwords are stored as BCrypt hashes.
- [ ] Unknown email and wrong password produce the same generic HTTP 401 response.
- [ ] Successful login returns an access token, expiry, and public user profile.
- [ ] Login sets a refresh cookie with the required attributes.
- [ ] Response bodies never expose refresh tokens.
- [ ] Expired and tampered access tokens are rejected.
- [ ] Refreshing rotates the refresh token.
- [ ] Expired, revoked, and reused refresh tokens are rejected.
- [ ] Refresh-token reuse triggers the approved revocation behavior.
- [ ] Refresh and logout reject requests missing the required CSRF header.
- [ ] Invalid request origins are rejected.
- [ ] Logout revokes the refresh token and clears the cookie.
- [ ] The current-user endpoint returns correct details without sensitive fields.
- [ ] Role changes produce the required audit record.
- [ ] Login rate limiting is tested.
- [ ] CORS does not combine wildcard origins with credentials.
- [ ] Automated tests pass.
- [ ] The API contract matches the implementation.

## 8. Out of Scope

- Social login.
- Public administrator registration.
- Frontend-only authorization.
- Plaintext password or refresh-token storage.
- Refresh-token storage in browser `localStorage`.

## 9. Definition of Done

B1 is complete when the approved requirements and API contract are implemented, security and integration tests pass, documentation is updated, and the changes are ready for code review.