# Git-Prasaaran JWT Strategy

## Purpose

Git-Prasaaran uses JWT-based authentication for protected human-user API
requests.

GitHub webhooks use a separate HMAC-SHA256 authentication mechanism.

## Access Tokens

Access tokens are short-lived JWTs.

The current access-token lifetime is:

* 15 minutes
* `expiresIn: 900` seconds in the login response

The token contains the claims required to authenticate the user and carry
role information.

Current claims include:

* `sub` — user email/username
* `iat` — issued-at timestamp
* `exp` — expiration timestamp
* `roles` — assigned application roles
* `iss` — configured issuer when present

JWT payloads do not contain passwords.

## Signing

The implementation uses HS256 (HMAC-SHA256).

The signing secret is supplied through environment/configuration and is never
committed to source control.

Example configuration:

```yaml
gitprasaaran:
  security:
    jwt:
      secret: ${JWT_SECRET}
```

The JWT implementation is responsible for generating and validating tokens
using the configured signing secret.

## Validation

Incoming bearer tokens are processed by the application's JWT
authentication filter.

For a protected request, the backend:

1. reads the bearer token from the `Authorization` header
2. validates the JWT signature
3. validates token validity and expiration
4. extracts the authenticated identity and roles
5. loads the corresponding user details
6. establishes the authenticated Spring Security principal

Invalid or expired tokens are not accepted.

Requests without valid authentication credentials receive HTTP 401 when the
target endpoint requires authentication.

## Session Strategy

Protected API authentication is stateless.

Spring Security uses:

```text
SessionCreationPolicy.STATELESS
```

The backend does not maintain a server-side login session for API
authentication.

Each protected request carries its own bearer token.

## Login

Users obtain an access token through:

```text
POST /api/v1/auth/login
```

The login flow uses Spring Security's `AuthenticationManager` to verify the
supplied email and password.

After successful authentication, the backend generates the JWT and returns:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

## Browser Token Strategy

The React frontend keeps the access token in application memory.

The current implementation does not persist the access token in
`localStorage`.

This avoids long-lived browser storage of the access token and keeps the
initial implementation simple and stateless.

A future refresh-token mechanism may use an `HttpOnly` and `Secure` cookie.
That mechanism is not part of the current Phase 7 implementation.

## CSRF

The API uses bearer-token authentication rather than cookie-based session
authentication.

The current Spring Security configuration ignores CSRF protection for:

```text
POST /api/v1/auth/login
/api/v1/webhooks/**
```

GitHub webhook authenticity is provided independently through HMAC-SHA256
signature verification.

If cookie-based refresh authentication is introduced in the future, its CSRF
requirements must be evaluated explicitly rather than globally disabling CSRF.

## Authorization and Roles

Application roles are persisted in PostgreSQL:

* CUSTOMER
* SUPPORT
* ADMIN

They are mapped to Spring Security authorities:

```text
ROLE_CUSTOMER
ROLE_SUPPORT
ROLE_ADMIN
```

Role information is carried by the JWT and restored into the authenticated
security context.

Phase 7 establishes the role infrastructure, but fine-grained
role-specific endpoint restrictions are future work. The current protected
API boundary primarily distinguishes authenticated from unauthenticated
requests.

## Public and Protected API Surface

Public endpoints include:

```text
GET  /api/v1/health
GET  /actuator/health
GET  /api/v1/documents/**
POST /api/v1/webhooks/**
POST /api/v1/auth/login
```

The following endpoint requires authentication:

```text
GET /api/v1/me
```

Other endpoints require authentication unless explicitly permitted by the
security configuration.

## Security Requirements

The implementation follows these requirements:

* JWT signing secrets must never be committed.
* JWT payloads must not contain passwords.
* Expired tokens must be rejected.
* Invalid signatures must be rejected.
* Bearer tokens must pass JWT validation before establishing authentication.
* Protected endpoints must fail closed.
* Authentication and authorization remain separate concerns.
* Access tokens are short-lived.
* Frontend access tokens are kept in application memory.

## Webhook Authentication

GitHub webhook authentication remains HMAC-SHA256 based.

A GitHub webhook does not require a Git-Prasaaran user JWT.

The webhook flow is:

```text
GitHub
  ↓
Webhook request
  ↓
X-Hub-Signature-256 verification
  ↓
Validate X-GitHub-Delivery
  ↓
Process push event
  ↓
Invalidate affected document cache entries
```

Webhook authentication is deliberately independent from human-user JWT
authentication.

## Verification

Phase 7 security verification includes:

### Backend

* security controller authentication tests
* unauthenticated request rejection
* authenticated request acceptance
* role exposure
* multiple-role handling

The full backend test suite passed with 59 tests.

### Frontend

* protected-route redirect test
* protected-route authenticated rendering test

The frontend security tests passed with 2 tests.

Frontend linting and production build also passed during Phase 7
verification.

## Future Hardening

The following are intentionally outside the current JWT implementation:

* refresh-token flow
* token revocation
* persistent browser authentication
* fine-grained endpoint role authorization
* production-grade secret rotation
* additional security hardening such as rate limiting and security-header
  policy

These can be addressed in later phases without changing the core stateless
JWT authentication model.
