# Git-Prasaaran Security Overview

This document summarizes the security controls, authorization model, and known security limitations of Git-Prasaaran.

For detailed design decisions, see:

- [Authentication Model](../docs/security-authentication-model.md)
- [JWT Strategy](../docs/security-jwt-strategy.md)
- [Agent Permissions](../docs/permissions.md)
- [AI Development Workflow](../docs/ai-development-workflow.md)

## Authentication and Authorization

Git-Prasaaran uses JWT-based authentication for protected API operations.

The application defines three roles:

- `CUSTOMER`
- `SUPPORT`
- `ADMIN`

Authorization rules determine which roles can access protected operations. Repository creation and updates are restricted to administrators under the current authorization model.

Public documentation endpoints and the GitHub webhook endpoint follow their configured security rules. Consult the authentication model for the exact endpoint boundaries.

## GitHub Webhook Security

GitHub webhook requests use HMAC-SHA256 signature verification.

Webhook processing also uses delivery identifiers to help prevent duplicate processing. Webhook secrets must be configured securely and must never be committed to source control.

## Documentation Content Security

Markdown content is parsed and rendered by the backend. HTML sanitization is used to reduce the risk of unsafe rendered content.

The application should continue to validate untrusted input and test rendering behavior as the supported Markdown features evolve.

## Secrets and Configuration

- Never commit passwords, API tokens, JWT signing secrets, or webhook secrets.
- Keep local environment files containing credentials out of version control.
- Use strong, environment-specific secrets outside local development.
- Do not expose private credentials through frontend variables prefixed with `VITE_`.
- Use development-only credentials for local Docker Compose services.
- Rotate credentials immediately if they are accidentally exposed.

## AI and MCP Security

The documentation Q&A assistant uses the configured Gemini integration.

The MCP server exposes read-only document tools:

- `list_documents`
- `get_document`
- `search_documents`

These tools do not provide document write operations. However, read-only tools can still expose any content accessible through their configured backend API.

Capability manifests and agent instructions document intended permissions. They are not substitutes for runtime authorization, process isolation, or a security sandbox.

## Known Limitations

- Public documentation endpoints are unauthenticated in the current implementation.
- Repository access is not scoped by user ownership.
- Read-only MCP operations inherit the backend document API's access model.
- Production deployment and production-grade secret management require additional configuration.
- This overview is not a substitute for an independent security audit.

These limitations should be reviewed before exposing the application to untrusted users or deploying it to production.

## Reporting Security Issues

Do not publish credentials, exploitable vulnerabilities, or sensitive operational details in public issues.

Report suspected security issues privately to the repository maintainer.