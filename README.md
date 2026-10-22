# Git-Prasaaran

## Git-Backed Markdown Documentation Publishing Engine

Git-Prasaaran transforms Markdown documents stored in Git repositories into secure, cached, and web-accessible documentation.

Git is the source of truth for published documentation. PostgreSQL stores application metadata and private user-owned Markdown; Redis caches rendered published documents.

---

## Live Demo

- **Frontend:** https://git-prasaaran.netlify.app/
- **Backend API:** https://git-prasaaran-backend.onrender.com/
- **Health Check:** https://git-prasaaran-backend.onrender.com/api/v1/health
- **Source Code:** https://github.com/PranayIngole7/git-prasaaran

> **Note:** The backend uses free-tier hosting. Initial requests may take longer after periods of inactivity.

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
- Local observability with Prometheus metrics, Grafana dashboards, and Alertmanager alert routing.
- A bounded, read-only incident responder with allowlisted alerts, diagnostic collection, and JSONL audit logging.

---

## Architecture

flowchart TB
    %% Clients
    subgraph CLIENTS["Clients and Integrations"]
        USER["User / Browser"]
        AI["AI Client"]
    end

    %% Application layer
    subgraph APP["Application Layer"]
        FRONTEND["React Frontend"]
        BACKEND["Spring Boot Backend<br/>REST API"]
        MCP["Read-only MCP Server"]
    end

    %% Data and integrations
    subgraph DATA["Data and External Integrations"]
        PG[("PostgreSQL<br/>Private User Documents")]
        REDIS[("Redis<br/>Cache")]
        GH["GitHub API<br/>Public Documentation"]
    end

    %% Observability
    subgraph OBS["Observability and Incident Response — Local Compose"]
        PROM["Prometheus<br/>Metrics Collection"]
        RULES["Alert Rules"]
        AM["Alertmanager"]
        IR["Bounded Incident Responder"]
        AUDIT[("JSONL Audit Log")]
        GRAFANA["Grafana<br/>Monitoring Dashboard"]
    end

    USER --> FRONTEND
    FRONTEND -->|"REST API requests"| BACKEND

    AI --> MCP
    MCP -->|"Read-only document access"| BACKEND

    BACKEND -->|"Private document storage"| PG
    BACKEND -->|"Cache operations"| REDIS
    BACKEND -->|"Public documentation"| GH

    BACKEND -.->|"Actuator metrics"| PROM
    PROM --> RULES
    RULES --> AM
    AM -->|"Alert events"| IR
    IR -->|"Read-only diagnostics"| PROM
    IR -->|"Audit records"| AUDIT
    PROM -->|"Metrics and queries"| GRAFANA

    classDef client fill:#e8f1ff,stroke:#356ac3,color:#172b4d
    classDef app fill:#e5f5eb,stroke:#27864b,color:#163d25
    classDef data fill:#fff0d9,stroke:#c47b17,color:#593600
    classDef monitoring fill:#f0e8ff,stroke:#8054bd,color:#352052

    class USER,AI client
    class FRONTEND,BACKEND,MCP app
    class PG,REDIS,GH data
    class PROM,RULES,AM,IR,AUDIT,GRAFANA monitoring

> GitHub webhooks notify the backend about documentation changes so relevant cache entries can be invalidated.

The local observability stack monitors backend scrape health and elevated HTTP 5xx rates. The incident responder accepts only configured alert names and performs bounded, read-only diagnostics; it does not execute remediation.

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
- **Observability:** Prometheus, Grafana, Alertmanager
- **Incident response:** Python standard-library HTTP responder with JSONL audit logging
- **CI:** GitHub Actions, including the incident-responder test suite

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

> The document endpoint requires valid GitHub configuration and access.

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

### Incident Responder Tests

The responder uses Python's standard library and does not require third-party Python packages:

```bash
PYTHONPATH=incident-responder python3 -m unittest discover \
  -s incident-responder/tests -p 'test_*.py' -v
```

### Run All Test Suites

From the project root, use the Makefile to run the backend, frontend, MCP, agent-hook, and incident-responder test suites:

```bash
make test-all
```

---

## Local Observability and Incident Response

The optional Docker Compose observability stack includes Prometheus, Grafana, and Alertmanager, alongside the application services and incident responder.

Set `GRAFANA_ADMIN_PASSWORD` in the root `.env` file before starting the full stack. Never commit or share that file.

```bash
docker compose config -q
docker compose up -d --build
docker compose ps
```

Local interfaces are bound to loopback:

- Frontend: `http://localhost:8082`
- Backend API: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Alertmanager: `http://localhost:9093`

The provisioned Grafana dashboard shows backend scrape status, request rate, HTTP 5xx rate, and JVM heap usage. Prometheus evaluates `BackendTargetDown` and `BackendHighServerErrorRate` rules and routes alerts to Alertmanager. Alertmanager sends webhook events to the bounded incident responder, which records sanitized audit events and whether human review is required.

**Limitations:** local Alertmanager-to-responder webhook delivery and audit logging have been tested. A real Prometheus rule-generated alert has not been verified end-to-end. The responder webhook has no application-level authentication and is intended only for a trusted local Compose environment. External email, Slack, and paging integrations are not configured. The responder does not perform automatic remediation or page a human operator.

For health checks, alert triage, troubleshooting, and responder safety boundaries, see the [Operations Guide](ops/README.md).

---

## Documentation

- **[Product specification](product-spec.md)**
- **[Architecture](docs/architecture.md)**
- **[API contract](openapi.yaml)**
- **[AI development workflow](docs/ai-development-workflow.md)**
- **[Authentication model](docs/security-authentication-model.md)**
- **[JWT strategy](docs/security-jwt-strategy.md)**
- **[Security overview](security/README.md)**
- **[Operations guide](ops/README.md)**
- **[Agent permissions](docs/permissions.md)**
- **[Agent extension pack](docs/agent-extension-pack.md)**
- **[Agent team and runtime safety boundaries](docs/agent-team.md)**

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
- The local incident responder performs bounded, read-only diagnostics and writes sanitized audit records; it does not execute automatic remediation.
- The responder webhook has no application-level authentication and must remain within a trusted local Compose environment.
- Local webhook delivery is tested, but external email/Slack/paging and end-to-end delivery of a real rule-generated Prometheus alert are not verified.
- Production uses free-tier hosting; production secret management and independent security auditing require additional work.

---

## License and Copyright

Copyright (c) 2026 Pranay Ingole. All rights reserved.

No open-source license has been granted for this project. You may view
the source code on GitHub, but permission to reuse, redistribute, or
create derivative works is not granted by this notice.

Third-party dependencies and components remain subject to their
respective licenses.

---