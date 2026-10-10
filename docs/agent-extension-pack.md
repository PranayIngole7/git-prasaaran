# Git-Prasaaran Agent Extension Pack

## 1. Purpose

The Git-Prasaaran agent extension pack defines how AI development tools can
interact with Git-Prasaaran through explicitly scoped capabilities.

The goal is to make documentation discovery and retrieval available to
compatible AI clients without granting unrestricted repository access,
arbitrary command execution, or production publishing privileges.

The extension follows the engineering and security rules in `AGENTS.md`.

## 2. Architecture

The current integration consists of:

- **MCP server:** A standalone TypeScript application using the official
  Model Context Protocol SDK.
- **Transport:** Standard input/output (stdio).
- **Backend integration:** HTTP requests to the existing Git-Prasaaran
  backend.
- **Content source:** Documentation served by the backend, ultimately
  backed by Git.
- **Configuration:** `BACKEND_BASE_URL`, defaulting to
  `http://localhost:8080`.

The MCP server is an integration layer. It does not replace the backend,
create a separate documentation database, or bypass backend behavior.

## 3. Implemented MCP Tools

### `list_documents`

Lists available documents.

Input: No required arguments.

Output: JSON document summaries containing:

- `slug`
- `title`
- `description`

Typical use cases include discovering available documentation and selecting
a document for further inspection.

### `get_document`

Retrieves a document by its slug.

Input:

- `slug`: Required, trimmed string with a maximum length of 200 characters.

Output includes the document's slug, title, description, Markdown content,
and a `truncated` indicator.

Document content is limited to 30,000 characters in the tool response.
Consumers should not assume that longer documents are returned in full.

An unknown slug is reported as a tool error.

### `search_documents`

Searches document metadata and Markdown content using a literal,
case-insensitive text query.

Input:

- `query`: Required non-empty string, maximum 200 characters.
- `limit`: Optional integer from 1 to 20; defaults to 10.

Results identify the matching field and provide a readable snippet.

This is an in-memory text search over documents returned by the backend.
It is not semantic search, embedding-based retrieval, or a vector database.
Results are limited to the documents available through the backend's
document-list endpoint.

## 4. Permissions and Safety Boundaries

The current MCP server is read-only.

It does not expose tools to:

- execute shell commands
- run arbitrary programs
- write or delete files
- create Git commits
- push to Git remotes
- modify repository settings
- write directly to PostgreSQL or Redis
- publish documentation
- bypass backend authentication or authorization

The server makes HTTP requests to the configured backend. The backend's
own security configuration remains relevant to the endpoints it exposes.

The current document endpoints used by this integration are public in the
application's existing security configuration. The MCP server should
therefore be treated as a controlled interface to publicly accessible
documentation, not as an authentication boundary.

Do not add protected or write-capable backend operations without reviewing
the permissions model and updating the security documentation and tests.

See [permissions.md](permissions.md).

## 5. Setup

Requirements:

- Node.js 22 or a compatible supported Node.js release
- npm
- A reachable Git-Prasaaran backend
