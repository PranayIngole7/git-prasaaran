# Git-Prasaaran Operations Guide

This guide covers local infrastructure, application startup, health checks, and common troubleshooting steps.

## Architecture and Ports

| Component           | Port | Purpose                         |
| ------------------- | ---: | ------------------------------- |
| React frontend      | 5173 | Browser interface               |
| Spring Boot backend | 8080 | REST API and application logic  |
| PostgreSQL          | 5432 | Persistent application metadata |
| Redis               | 6379 | Documentation cache             |

The backend retrieves Markdown documentation from the configured GitHub repository. PostgreSQL stores application metadata, while Redis caches documentation data.

## Prerequisites

Install:

- Git
- Java 21
- Node.js 22 and npm
- Docker Engine
- Docker Compose

## 1. Start Infrastructure

From the project root, run:

```bash
docker compose up -d postgres redis
```

Check container status:

```bash
docker compose ps
```

Inspect service logs if necessary:

```bash
docker compose logs postgres redis
```

## 2. Configure and Start the Backend

Configure the required environment variables in the backend terminal.

Example local configuration:

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

Replace the GitHub placeholders with valid repository details. Configure `GITHUB_TOKEN` when required by the repository's access settings.

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Flyway applies database migrations during application startup.

Do not reuse development credentials or a development JWT secret in production.

## 3. Start the Frontend

Open a separate terminal:

```bash
cd frontend
npm ci
cp .env.example .env.local
```

Set the API URL in `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Start the frontend:

```bash
npm run dev
```

Open the local URL printed by Vite, normally `http://localhost:5173`.

## 4. Start the MCP Server

Open another terminal:

```bash
cd mcp-server
npm ci
npm run build
BACKEND_BASE_URL=http://localhost:8080 npm start
```

The MCP server uses stdio transport. An MCP-compatible client should launch it and communicate through standard input and output.

Available tools:

- `list_documents`
- `get_document`
- `search_documents`

The tools are read-only. Their effective access is still subject to the configured backend API and document access model.

## 5. Health Checks

Check the application health endpoint:

```bash
curl -i http://localhost:8080/api/v1/health
```

Check Spring Boot Actuator, if enabled:

```bash
curl -i http://localhost:8080/actuator/health
```

Check the documentation API:

```bash
curl -i http://localhost:8080/api/v1/documents
```

The documentation endpoint depends on valid GitHub configuration and repository access.

## 6. Run Tests

Backend:

```bash
cd backend
./mvnw --batch-mode test
```

Frontend:

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

MCP server:

```bash
cd mcp-server
npm ci
npm test
npm run build
```

Agent hook:

```bash
node --test agent-hooks/tests/validate-markdown.test.mjs
```

Run the agent hook test from the project root.

## Troubleshooting

### Backend cannot connect to PostgreSQL

- Confirm the PostgreSQL container is running with `docker compose ps`.
- Check PostgreSQL logs using `docker compose logs postgres`.
- Verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Confirm PostgreSQL is reachable on port `5432`.

### Backend cannot connect to Redis

- Confirm Redis is running.
- Check its logs with `docker compose logs redis`.
- Verify `REDIS_HOST` and `REDIS_PORT`.

### Backend fails during startup

- Check that Java 21 is installed.
- Review the backend startup logs.
- Verify the required `JWT_SECRET` is configured.
- Confirm database connectivity and Flyway migration results.

### Documents cannot be retrieved

- Verify `GITHUB_OWNER`, `GITHUB_REPOSITORY`, `GITHUB_BRANCH`, and `GITHUB_CONTENT_PATH`.
- Check repository visibility and token permissions.
- Confirm that the configured content path contains Markdown documents.
- Review backend logs for GitHub API errors.

### Frontend cannot reach the backend

- Verify that the backend is running on port `8080`.
- Check `VITE_API_BASE_URL` in `frontend/.env.local`.
- Confirm that the configured CORS origin matches the frontend URL.
- Restart the frontend development server after changing environment variables.

### MCP tools return errors

- Confirm that the backend is running and its document API is accessible.
- Verify `BACKEND_BASE_URL`.
- Rebuild the MCP server after source changes.
- Check the MCP client's process configuration and standard error output.

## Shutdown

Stop the frontend and backend using `Ctrl+C` in their respective terminals.

Stop the local infrastructure from the project root:

```bash
docker compose down
```

This stops the containers but preserves their volumes unless they are explicitly removed.

Avoid deleting database volumes unless you intend to discard the associated local data.