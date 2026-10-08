# Git-Prasaaran Authentication Model

## Purpose

Git-Prasaaran uses application-level authentication for protected human-user
APIs while preserving HMAC-based authentication for GitHub webhooks.

Human-user authentication and webhook authentication are intentionally
separate security mechanisms.

## User Identity

Git-Prasaaran users are application users stored in PostgreSQL.

A user has:

* unique identifier
* email/username
* password hash
* enabled/disabled state
* roles
* creation metadata

Plaintext passwords are never stored.

Passwords are verified using Spring Security with BCrypt-backed password
hashes.

## Authentication

Human users authenticate with application credentials through:

```text
POST /api/v1/auth/login
```

The backend:

1. receives the user's email and password
2. delegates credential verification to Spring Security
3. loads the user and roles from PostgreSQL
4. verifies the supplied password against the stored BCrypt hash
5. establishes an authenticated Spring Security principal
6. issues a short-lived JWT access token

A successful login returns a bearer access token with a 15-minute lifetime.

## API Authentication Strategy

Git-Prasaaran uses stateless JWT-based authentication for protected API
requests.

Authenticated requests provide the token using the HTTP Authorization header:

```text
Authorization: Bearer <access-token>
```

The backend validates the JWT for each authenticated request.

The JWT contains the information required to establish the authenticated
principal, including the user's identity and roles.

The backend does not maintain a server-side login session.

## Authorization

Authentication answers:

> Who is the user?

Authorization answers:

> What is the user allowed to do?

Git-Prasaaran has persisted application roles:

* CUSTOMER
* SUPPORT
* ADMIN

These roles are mapped to Spring Security authorities:

```text
ROLE_CUSTOMER
ROLE_SUPPORT
ROLE_ADMIN
```

Role information is available to the authenticated security context and is
included in issued JWTs.

Authorization is currently configured with URL-based Spring Security
request matchers in `SecurityConfig`; method-level authorization is not used.
The repository API rules are:

* authenticated users may list and view repositories
* only users with the `ADMIN` role may create or update repositories
* updating `active` to `false` deactivates a repository
* repository deletion is not supported

Repository authorization is not ownership-based. There are no user-to-
repository ownership or multi-user access relationships yet.

## Protected API Boundaries

The following endpoints are intentionally public:

```text
GET  /api/v1/health
GET  /actuator/health
GET  /api/v1/documents/**
POST /api/v1/webhooks/**
POST /api/v1/auth/login
```

The current authenticated-user endpoint is:

```text
GET /api/v1/me
```

It requires a valid JWT and returns the authenticated user's email and
assigned roles.

Repository endpoints are protected by the same JWT filter and URL-based
authorization rules:

```text
GET   /api/v1/repositories
GET   /api/v1/repositories/{repositoryId}
GET   /api/v1/repositories/{repositoryId}/documents
GET   /api/v1/repositories/{repositoryId}/documents/{slug}
POST  /api/v1/repositories                         ADMIN only
PATCH /api/v1/repositories/{repositoryId}           ADMIN only
```

Repository reads require authentication. Repository creation and updates,
including deactivation through `active=false`, require `ADMIN`. No repository
DELETE operation exists. These rules do not change the existing public
document endpoints or webhook signature authentication.

Endpoints not explicitly configured as public require authentication.

Unauthenticated access to protected endpoints returns HTTP 401.

## Frontend Authentication State

The React frontend maintains the active access token in application memory.

The token is not persisted in `localStorage`.

The frontend authentication context tracks:

* access token
* current authenticated user
* authentication state
* login operation
* logout operation

After login, the frontend uses the access token to request the current
user identity from:

```text
GET /api/v1/me
```

Logging out clears the in-memory token and authenticated user state.

## Frontend Protected Routes

Protected frontend routes use a dedicated `ProtectedRoute` component.

Unauthenticated users are redirected to:

```text
/login
```

The originally requested pathname is preserved in router state so the
application can retain navigation context.

API responses with HTTP 401 clear the frontend authentication state and
redirect the user to the login page.

HTTP 403 responses are routed to the application's forbidden state.

## GitHub Webhook Authentication

GitHub webhooks use HMAC-SHA256 signature verification.

Webhook authentication is intentionally separate from human-user
authentication.

The webhook endpoint does not require a Git-Prasaaran user JWT.

Webhook processing also protects against duplicate GitHub deliveries using
the `X-GitHub-Delivery` identifier.

## Security Boundaries

* PostgreSQL is the source of truth for application users and roles.
* Passwords are stored only as secure password hashes.
* Frontend code does not receive backend secrets.
* JWT signing secrets are supplied through configuration/environment variables.
* JWT validation occurs on the backend.
* Protected API endpoints fail closed.
* Webhook requests are authenticated using their GitHub signature.
* Human-user authentication and webhook authentication remain separate.
* Authentication and authorization remain separate concerns.
* Repository access is currently authenticated globally, not scoped by
  ownership or user-to-repository relationships.
* Repository configuration responses do not expose GitHub credentials or
  tokens.
* Access tokens are kept in frontend memory rather than browser
  localStorage.

## Authentication Flow

```text
User
  ↓
Login request
  ↓
POST /api/v1/auth/login
  ↓
Spring Security AuthenticationManager
  ↓
Load user from PostgreSQL
  ↓
Verify BCrypt password hash
  ↓
Issue 15-minute JWT
  ↓
Frontend application memory
  ↓
Authorization: Bearer <token>
  ↓
Protected API
  ↓
JWT authentication filter
  ↓
Validate token
  ↓
Load authenticated principal
  ↓
Controller
```

## Security Failure Handling

The backend returns HTTP 401 for requests that do not provide valid
authentication credentials.

The frontend responds to HTTP 401 by clearing its authentication state and
redirecting to `/login`.

HTTP 403 is used when an authenticated request is forbidden by the security
configuration.

## Verification

Phase 7 includes automated security verification.

Backend security tests cover:

* unauthenticated access to `/api/v1/me`
* authenticated access to `/api/v1/me`
* authenticated role information
* multiple-role handling

Frontend security tests cover:

* redirecting unauthenticated users from protected routes
* rendering protected content for authenticated users

The backend and frontend security test suites passed during Phase 7
verification.
