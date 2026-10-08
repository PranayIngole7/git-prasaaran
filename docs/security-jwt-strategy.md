# Git-Prasaaran JWT Strategy

## Purpose

Git-Prasaaran uses JWT-based authentication for protected human-user API
requests.

GitHub webhooks use a separate HMAC-SHA256 authentication mechanism.

## Access Tokens

Access tokens are short-lived JWTs.

Default target lifetime:

- 15 minutes

The token contains only the minimum claims required for authentication and
authorization.

Expected claims:

- `sub` — user identifier
- `iat` — issued-at timestamp
- `exp` — expiration timestamp
- `roles` — user roles
- `iss` — issuer, when issuer validation is enabled

## Signing

The initial implementation uses HS256 (HMAC-SHA256).

The signing secret is supplied through environment/configuration and is never
committed to source control.

Example configuration:

```yaml
gitprasaaran:
  security:
    jwt:
      secret: ${JWT_SECRET}
```

## Session Strategy

Protected API authentication is stateless.

The backend will validate the JWT on each authenticated API request rather
than maintaining a server-side login session.

Spring Security will use stateless session management once JWT request
authentication is implemented.

## Browser Token Strategy

Access tokens should be short-lived and handled in application memory rather
than persisted in localStorage.

A future refresh-token mechanism may use an HttpOnly, Secure cookie.

The refresh-token design is intentionally separate from the initial access
token implementation.

## CSRF

Bearer access-token requests do not rely on automatically attached browser
cookies.

Cookie-based refresh operations require deliberate CSRF protection and will
not be implemented by globally disabling CSRF.

## Security Requirements

- JWT signing secrets must never be committed.
- JWT payloads must not contain passwords or sensitive private data.
- Expired tokens must be rejected.
- Invalid signatures must be rejected.
- Tokens must not be accepted without successful signature validation.
- Protected endpoints must fail closed.
- Authentication and authorization remain separate concerns.

## Webhook Authentication

GitHub webhook authentication remains HMAC-SHA256 based.

A GitHub webhook does not require a Git-Prasaaran user JWT.

## Future Work

Implementation of:

- JWT generation
- JWT validation
- login endpoint
- password hashing
- refresh tokens
- role-based authorization
  
is handled by subsequent Phase 7 subphases.
---