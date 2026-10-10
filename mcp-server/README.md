# Git-Prasaaran MCP Server

## 1. Overview

The Git-Prasaaran MCP server exposes repository documentation through the Model Context Protocol (MCP).

It allows compatible AI clients to discover, retrieve, and search documents provided by the Git-Prasaaran backend.

The server is implemented in TypeScript and communicates with MCP clients through standard input and standard output (stdio).

## 2. Architecture

The request flow is:

1. An MCP-compatible client invokes a tool.
2. The MCP server validates the tool arguments.
3. The server calls the Git-Prasaaran backend document API.
4. The server returns a result to the MCP client.

The backend remains responsible for retrieving documentation. The MCP server does not access the database or Git repository directly.

## 3. Available Tools

### `list_documents`

Lists available documents and returns metadata such as slug, title, and description.

### `get_document`

Retrieves a document by its slug.

The result includes document content, limited to 30,000 characters. If the content exceeds this limit, the result indicates that it was truncated.

### `search_documents`

Searches available document content and metadata using literal, case-insensitive text matching.

Inputs:

- `query` — required search text, up to 200 characters.
- `limit` — maximum number of results, from 1 to 20; defaults to 10.

Results include matching documents and a short context snippet.

This is literal text search, not semantic search or embedding-based retrieval.

## 4. Requirements

Install Node.js and npm.

The server dependencies are defined in `package.json` and locked in `package-lock.json`.

## 5. Configuration

The server supports the following environment variable:

| Variable | Default | Description |
| --- | --- | --- |
| `BACKEND_BASE_URL` | `http://localhost:8080` | Base URL of the Git-Prasaaran backend |

The configured URL must use HTTP or HTTPS.

Ensure that the backend is running and its document API is reachable before invoking the MCP tools.

## 6. Install, Build, and Test

Run these commands from the `mcp-server` directory:

```bash
npm ci
npm run build
npm test
```

To start the server:

npm start

The server communicates through stdio. Starting it in a terminal confirms startup, but using its tools requires an MCP-compatible client connected to the server.

## 7. Connecting an MCP Client

Configure your MCP-compatible client to launch the compiled server entry point:

`node /absolute/path/to/git-prasaaran/mcp-server/dist/index.js`

Replace the example path with the actual absolute path to your project. Build the project before launching the compiled entry point.

If the backend is not available at `http://localhost:8080`, configure `BACKEND_BASE_URL` in the MCP client's environment. The exact configuration format depends on the client, so consult its documentation.

## 8. Security and Limitations

The current MCP server is read-only. It does not provide arbitrary shell execution, filesystem writes, Git operations, direct database access, document creation or modification, or production publishing.

The server calls the backend's document API. The current document endpoints are public, so the MCP server must not be treated as an authentication or authorization boundary.

Run the server in an appropriately controlled environment. Review network exposure, backend access, and client permissions before using it in production.

Treat retrieved document content as untrusted input. Do not commit credentials or local environment files containing secrets.

## 9. Project Documentation

- [Repository instructions](../AGENTS.md)
- [Product specification](../product-spec.md)
- [Architecture](../docs/architecture.md)
- [Agent extension pack](../docs/agent-extension-pack.md)
- [Permissions and security](../docs/permissions.md)
- [AI development workflow](../docs/ai-development-workflow.md)
- [OpenAPI contract](../openapi.yaml)
