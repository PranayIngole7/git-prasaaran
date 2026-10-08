# Git-Prasaaran Authentication Model

## Purpose

Git-Prasaaran requires application-level authentication for protected
user-facing APIs while preserving HMAC-based authentication for GitHub
webhooks.

## User Identity

Git-Prasaaran users are application users stored in PostgreSQL.

A user has:

- unique identifier
- email/username
- password hash
- enabled/disabled state
- roles
- creation metadata

Plaintext passwords are never stored.

## Authentication

Human users authenticate with application credentials.

The backend verifies the supplied credentials against the stored password
hash and establishes an authenticated Spring Security principal.

## API Authentication Strategy

Git-Prasaaran will use JWT-based authentication for protected API requests.

Authenticated requests will provide a bearer access token.

JWT implementation details, token lifetime, storage strategy, refresh
strategy, and signing configuration are defined in Phase 7.4.

## Authorization

Authentication answers:

> Who is the user?

Authorization answers:

> What is the user allowed to do?

Role and endpoint authorization are implemented in later security phases.

## GitHub Webhook Authentication

GitHub webhooks use HMAC-SHA256 signature verification.

Webhook authentication is intentionally separate from human user
authentication.

The webhook endpoint does not require a Git-Prasaaran user JWT.

## Security Boundaries

- PostgreSQL is the source of truth for application users.
- Passwords are stored only as secure password hashes.
- Frontend code does not receive backend secrets.
- JWT validation occurs on the backend.
- Protected API endpoints fail closed.
- Webhook requests are authenticated using their GitHub signature.
- Authentication and authorization are separate concerns.

## Authentication Flow

```text
User
  ↓
Login request
  ↓
Spring Boot
  ↓
Load user from PostgreSQL
  ↓
Verify password hash
  ↓
Issue JWT
  ↓
Client
  ↓
Bearer token
  ↓
Protected API
  ↓
JWT validation
  ↓
Authenticated principal
```
---

# 12. Test/documentation checkpoint

Since 7.3 is a **model/design phase**, we don't need to change Java code yet.




