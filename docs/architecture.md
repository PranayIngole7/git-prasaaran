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

## Application Repository Context

A Git-Prasaaran Repository is an application-level configuration and context
boundary with a stable internal ID. Its PostgreSQL metadata identifies the
external GitHub owner, repository name, branch, and content path to use for
document retrieval. The external GitHub repository remains the source of
truth for the Markdown content; PostgreSQL does not store that content.

Repository-scoped document requests resolve the selected application
Repository before retrieval. The context is passed explicitly through the
document service and GitHub integration.

## Repository-Scoped Document Retrieval

The repository-scoped API is:

```text
GET /api/v1/repositories/{repositoryId}/documents
GET /api/v1/repositories/{repositoryId}/documents/{slug}
```

The list operation retrieves documents using the selected repository
configuration and does not use list caching. A single-document request uses
the repository-scoped cache described below.

```text
Repository-scoped document request
  ↓
Resolve application Repository by internal ID (PostgreSQL metadata)
  ↓
Use owner/name/branch/contentPath as explicit retrieval context
  ↓
Single document? ── yes ──→ Redis lookup: document:{repositoryId}:{slug}
  │                                      │
  │                                      └─ hit ──→ API response
  │
  └─ List, or single-document cache miss
        ↓
      GitHub document retrieval
        ↓
   Markdown parsing and HTML rendering
        ↓
   HTML sanitization
        ↓
   Single document? ── yes ──→ Write repository-scoped Redis cache
        ↓
   Repository-scoped API response
```

Redis is only a cache. A repository-specific key includes the stable internal
repository ID, so documents with the same slug from different application
repositories do not share cache entries. The existing global document
retrieval path remains available for the legacy `/api/v1/documents` routes.

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

Webhook cache invalidation is not repository-scoped. For a signature-verified
push delivery, the backend resolves the payload's owner and repository name
against configured application Repositories. An event is associated only when
exactly one repository matches; unmatched or ambiguous events remain
unassociated. Historical events are not backfilled and retain a null
repository association. Delivery-ID deduplication and the existing received,
processed, and failed statuses are preserved.

Authenticated activity feeds expose persisted webhook event metadata through
`GET /api/v1/activity` and
`GET /api/v1/repositories/{repositoryId}/activity`. The global feed can include
historical or unmatched events with no repository association; the
repository-scoped feed includes only events associated with that internal
repository ID. Activity records do not include commit messages, actors,
changed files, branch names, or cache invalidation counts because those
details are not persisted.

## Future Agent Context

Future AI, MCP, and agent capabilities are expected to receive an explicit
application Repository context, rather than relying on an implicit global
GitHub repository. Those capabilities are not implemented by the repository
document API.

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