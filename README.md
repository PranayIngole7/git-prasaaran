# Git-Prasaaran

## Git-Backed Markdown Documentation Publishing Engine

Git-Prasaaran transforms Markdown documents stored in Git repositories into secure, cached, and web-accessible documentation.

Git is the source of truth for published documentation. PostgreSQL stores application metadata and private user-owned Markdown; Redis caches rendered published documents.
---

## Features

- GitHub-backed Markdown documentation.
- Private Markdown documents owned by authenticated users.
- YAML front matter parsing and HTML sanitization.
- REST API and React documentation interface.
- Redis caching and GitHub webhook cache invalidation.
- JWT authentication and role-based authorization.
- Repository management and activity feeds.
- AI-assisted documentation Q&A through the configured Gemini integration.
- Read-only MCP tools for listing, retrieving, and searching documents.
- Agent extension pack with capability manifests, custom agent instructions, and a Markdown validation hook.
---

## Architecture

GitHub Repository → Spring Boot Backend → Redis and GitHub API

Private user documents → Spring Boot Backend → PostgreSQL

React Frontend → Spring Boot Backend

AI Client → Read-only MCP Server → Backend Document API

GitHub webhooks notify the backend about documentation changes so relevant cache entries can be invalidated.
---

## Technology Stack

- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA
- **Database:** PostgreSQL 16 and Flyway
- **Cache:** Redis 7
- **Frontend:** React, TypeScript, Vite, Tailwind CSS
- **API contract:** OpenAPI
- **AI assistant:** Gemini API integration
- **Agent integration:** TypeScript and Model Context Protocol SDK
- **Testing:** JUnit, Spring testing, Vitest, Node.js test runner
- **Local infrastructure:** Docker Compose
- **CI:** GitHub Actions
---

## Prerequisites

Install the following tools:

- Git
- Java 21
- Node.js 22 and npm
- Docker Engine
- Docker Compose
---

## Local Setup

### 1. Start Infrastructure

From the project root, run:

```bash
docker compose up -d postgres redis
docker compose ps
```

### 2. Configure the Backend

Set the following environment variables in the terminal where you will start the backend:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/gitprasaaran
export DB_USERNAME=gitprasaaran
export DB_PASSWORD=gitprasaaran
export REDIS_HOST=localhost
export REDIS_PORT=6379
export SERVER_PORT=8080
export CORS_ALLOWED_ORIGINS=http://localhost:5173
export JWT_SECRET="$(openssl rand -base64 32)"
export GITHUB_OWNER=YOUR_GITHUB_OWNER
export GITHUB_REPOSITORY=YOUR_DOCUMENTATION_REPOSITORY
export GITHUB_BRANCH=main
export GITHUB_CONTENT_PATH=docs
export GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```
> `GEMINI_API_KEY` is optional for local startup but must be configured to use the Gemini-powered documentation Q&A feature.

- Start the backend:
```bash
cd backend
./mvnw spring-boot:run
```
### 3. Start the Frontend

- Open another terminal and run:
```bash
cd ~/ai-dev-tool-zoomcamp/projects/git-prasaaran/frontend
npm ci
cp .env.example .env.local
```

- Set the following variable in frontend/.env.local:
```bash
VITE_API_BASE_URL=http://localhost:8080
```

- Start the frontend:
```bash
npm run dev
```

- Open the URL printed by Vite, normally:

> http://localhost:5173

### 4. Start the MCP Server

Open another terminal and run:
```bash
cd ~/ai-dev-tool-zoomcamp/projects/git-prasaaran/mcp-server
npm ci
npm run build
BACKEND_BASE_URL=http://localhost:8080 npm start
```
The MCP server uses standard input/output (stdio) transport and is intended to be launched by an MCP-compatible client.

**Available tools:**

- `list_documents`
- `get_document`
- `search_documents`

Search is literal and case-insensitive, not semantic.

### 5. Verify the Backend

With the backend running, execute:
```bash
curl -i http://localhost:8080/api/v1/health
```

Check the Spring Boot Actuator health endpoint:
```bash
curl -i http://localhost:8080/actuator/health
```

Check the documentation endpoint:
```bash
curl -i http://localhost:8080/api/v1/documents
```

The document endpoint requires valid GitHub configuration and access.
---

## Testing

### Backend Tests
```bash
cd backend
./mvnw --batch-mode test
```

### Frontend Tests, Lint, and Production Build
```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

### MCP Tests and Build
```bash
cd mcp-server
npm ci
npm test
npm run build
```

### Agent Hook Tests

Run from the project root:
```bash
node --test agent-hooks/tests/validate-markdown.test.mjs
```
---

## Documentation
**Product specification:** `product-spec.md`
**Architecture:** `docs/architecture.md`
**API contract:** `openapi.yaml`
**AI development workflow:** `docs/ai-development-workflow.md`
**Authentication model:** `docs/security-authentication-model.md`
**JWT strategy:** `docs/security-jwt-strategy.md`
**Security overview:** `security/README.md`
**Operations guide:** `ops/README.md`
**Agent permissions:** `docs/permissions.md`
**Agent extension pack:** `docs/agent-extension-pack.md`
---

## Security and Limitations
- Never commit credentials, signing secrets, or production environment files.
- Local Docker Compose credentials are for development only.
- Published GitHub document endpoints are public; private-document endpoints require JWT authentication and enforce per-user ownership.
- Protected endpoints use JWT authentication and configured authorization rules.
- GitHub webhooks use HMAC-SHA256 signature verification.
- Configured GitHub repository access is not scoped by user ownership. Private-document ownership is enforced separately.
- MCP exposes read-only documentation tools but inherits the backend document API's access model.
- The capability manifest and agent instructions describe intended permissions; they are not a security sandbox.
- Production deployment, secret management, and independent security auditing require additional work.
---

## License

No license has been declared. Contact the repository owner before redistributing this project.
---