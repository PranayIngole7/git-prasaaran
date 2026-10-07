# Git-Prasaaran Architecture

## Architecture Style

Git-Prasaaran uses a modular full-stack architecture.

The backend is implemented as a Spring Boot application with clear separation between API, application, domain/processing, and infrastructure concerns.

Microservices are intentionally out of scope for V1.

## High-Level Architecture

```text
GitHub Repository
      │
      │ Webhook
      ▼
Spring Boot Backend
      │
      ├── PostgreSQL
      │     └── Application metadata
      │
      ├── Redis
      │     └── Rendered document cache
      │
      └── GitHub API
            └── Markdown source

React + Vite
      │
      ▼
Spring Boot REST API

MCP Server
      │
      ▼
Controlled Git-Prasaaran capabilities
```

## Source of Truth

Git/GitHub is the source of truth for Markdown documentation.

PostgreSQL stores application metadata only.

Redis stores cached processed documents.

## Document Retrieval
```text
Request
  ↓
Redis lookup
  ↓
Cache hit ──────→ Response
  │
  └─ Cache miss
        ↓
      GitHub
        ↓
   Markdown parsing
        ↓
   HTML rendering
        ↓
   HTML sanitization
        ↓
      Redis
        ↓
     Response
```

## Webhook Processing
```text
Git push
   ↓
GitHub webhook
   ↓
HMAC verification
   ↓
Changed document detection
   ↓
Webhook event recording
   ↓
Redis cache invalidation
```

## Backend Boundaries
```text
api/
    HTTP controllers and request/response models

application/
    Use cases and orchestration

domain/
    Core document/repository concepts

infrastructure/
    GitHub, PostgreSQL, Redis and Markdown integrations
```

## Security Boundaries
- GitHub webhook signatures are verified.
- Rendered HTML is sanitized.
- Secrets are supplied through environment/configuration.
- Frontend does not receive backend credentials.
- MCP capabilities are explicitly scoped.
- Arbitrary shell execution is not exposed through MCP.

## Architecture Principles
1. Git is the content source of truth.
2. PostgreSQL stores metadata.
3. Redis is a cache.
4. Controllers do not contain business logic.
5. External integrations remain isolated.
6. Security validation cannot be bypassed for convenience.
7. Infrastructure complexity must be justified by a real requirement.