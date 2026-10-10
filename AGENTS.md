# Git-Prasaaran — AI Development Guidelines

## Project

Git-Prasaaran is a Git-backed Markdown documentation publishing engine.

Git is the source of truth for documentation content.

## Engineering Principles

1. Keep the MVP small and focused.
2. Prefer simple, maintainable solutions over unnecessary infrastructure.
3. Keep business logic out of controllers.
4. Keep PostgreSQL for application metadata and explicitly user-owned private
   documents; Git remains the source of truth for published Markdown.
5. Treat Redis as a cache, never as the source of truth.
6. Validate all GitHub webhook signatures.
7. Sanitize rendered HTML.
8. Never commit credentials or secrets.
9. Do not introduce infrastructure that is not justified by the product requirements.
10. Prefer measurable claims over unsupported performance claims.

## Architecture Boundaries

The backend should maintain clear separation between:

- API/controllers
- application services
- domain/processing logic
- infrastructure integrations

External integrations should be isolated behind appropriate interfaces where useful.

## AI-Assisted Development

AI tools may assist with:

- code generation
- refactoring
- documentation
- test generation
- debugging
- architecture exploration
- security review

AI-generated changes must be reviewed by a human before being accepted.

Every significant AI-generated change must be verified through appropriate tests, static analysis, manual inspection, or runtime verification.

AI output must not be treated as automatically correct.

## Security Rules

Never:

- commit secrets
- expose backend credentials to the frontend
- disable security validation merely to make tests pass
- bypass webhook signature verification
- introduce unrestricted shell execution
- provide unrestricted repository access to an agent
- store published Markdown content in PostgreSQL; private user documents are
  the explicit owner-scoped exception

## MCP / Agent Rules

MCP capabilities must be explicitly scoped and documented.

Agents should only receive the minimum permissions required for their task.

Production publishing must remain human-controlled.

Arbitrary shell execution must not be exposed through MCP.

## Dependency Rules

Before adding a dependency:

1. Confirm that it solves a real project requirement.
2. Prefer established libraries.
3. Avoid duplicate libraries providing the same functionality.
4. Verify compatibility with the project stack.
5. Document important architectural decisions.

## Testing Rules

New functionality should include appropriate tests.

Before considering a milestone complete:

- run relevant unit tests
- run integration tests where applicable
- run build verification
- inspect failures rather than suppressing them

## Git Rules

Use small, meaningful commits.

Preferred commit style:

```text
type: concise description
```