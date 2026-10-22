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

## 7. Docker Compose Observability

The root Docker Compose setup runs the frontend, backend, PostgreSQL,
Redis, Prometheus, Grafana, Alertmanager, and the incident responder.
The responder is internal-only and does not publish a host port. Host ports
differ from the development ports documented earlier.

| Component   | Host address            | Purpose                           |
| ----------- | ----------------------- | --------------------------------- |
| Frontend    | `http://localhost:8082` | Docker-hosted browser UI          |
| Backend API | `http://localhost:8081` | REST API; container port 8080     |
| PostgreSQL  | `localhost:5432`        | Persistent application metadata   |
| Redis       | `localhost:6379`        | Documentation cache               |
| Prometheus  | `http://localhost:9090` | Metrics and alert-rule evaluation |
| Grafana     | `http://localhost:3000` | Service dashboard                 |
| Alertmanager| `http://localhost:9093` | Alert routing                     |
| Responder   | Internal Compose network only | Bounded diagnostics and audit log |

The backend's Actuator management server listens on container port `8081`.
That management port is not published directly to the host. Therefore,
`http://localhost:8081/actuator/health` from the host reaches the main
application port, not the management server.

### Start and verify the stack

From the repository root:

```bash
docker compose config -q
docker compose up -d --build
docker compose ps
```

Check the application API and management health separately:

```bash
curl -i http://127.0.0.1:8081/api/v1/health

docker compose exec backend sh -lc \
  'wget -S -O- http://127.0.0.1:8081/actuator/health'
```

Check Prometheus and Grafana:

```bash
curl -fsS http://127.0.0.1:9090/-/healthy
curl -fsS http://127.0.0.1:9090/-/ready
curl -fsS http://127.0.0.1:3000/api/health
```

Prometheus should scrape the `git-prasaaran-backend` target at
`backend:8081`. Confirm that the target is up:

```bash
curl -fsS -G http://127.0.0.1:9090/api/v1/query \
  --data-urlencode 'query=up{job="git-prasaaran-backend"}'
```

A value of `1` means the latest scrape succeeded; `0` means the target
is currently failing to scrape.

### Grafana access

Open `http://localhost:3000`. The provisioned dashboard is titled
**Git-Prasaaran — Service Overview**, in the **Git-Prasaaran** folder.

The initial administrator password is configured using
`GRAFANA_ADMIN_PASSWORD` in the root `.env` file. Keep `.env` private,
never commit it, and never paste its contents into issues or logs.

The dashboard includes backend scrape status, request rate, HTTP 5xx
rate, and JVM heap usage. Allow time for Prometheus to collect samples
before interpreting rate-based graphs.

### Alert rules

Prometheus loads alert rules from `monitoring/alerts.yml`.

| Alert                        | Meaning                                                                   | Initial response                                                           |
| ---------------------------- | ------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `BackendTargetDown`          | Backend metrics target is unavailable for at least one minute             | Check backend health, container status, logs, and Prometheus target status |
| `BackendHighServerErrorRate` | HTTP 5xx rate is above 5% for five minutes with sufficient request volume | Inspect recent backend errors, dependencies, and affected request paths    |

Inspect rule state and health:

```bash
curl -fsS http://127.0.0.1:9090/api/v1/rules
```

Inspect target status:

```bash
curl -fsS http://127.0.0.1:9090/api/v1/targets
```

Inspect backend logs:

```bash
docker compose logs --since=10m --tail=200 backend
```

Inspect recent container health:

```bash
docker compose ps
docker compose logs --since=10m --tail=100 postgres redis
```

An alert in the `inactive` state is not necessarily an error: it can mean
the condition is not currently met. Check the rule's `health` field and
Prometheus target status as well.

**Notification and delivery status:** Prometheus sends alerts to the
local Alertmanager service, which routes webhook events to the bounded
incident responder. Local Alertmanager-to-responder delivery and audit
logging have been tested. External email, Slack, and other human paging
integrations are not configured. The responder records escalation
requirements but does not page an external recipient. Do not assume an
alert has reached a human operator.

## 8. Incident Response Runbook

Use this procedure for local development and test environments. For a
production incident, follow the approved production incident process
and involve the responsible human operator.

### Phase A — Triage

1. Record the alert name, observed time, and affected component.
2. Check `docker compose ps`.
3. Check Prometheus target status and alert-rule health.
4. Check the backend health endpoint and recent logs.
5. Check PostgreSQL and Redis health if backend errors suggest dependency
   failures.
6. Establish whether the issue is ongoing or was a transient test failure.

Do not include access tokens, JWTs, API keys, credentials, or private
document contents in incident reports.

### Phase B — Diagnosis

For `BackendTargetDown`:
- Verify that the backend container is running and healthy.
- Inspect backend logs for startup, configuration, database, or Redis errors.
- Verify that the Prometheus target is `backend:8081`.
- Check the internal management endpoint using `docker compose exec backend`.
- Do not treat an API request to the main application port as an Actuator
  health check.

For `BackendHighServerErrorRate`:
- Inspect the backend logs around the alert timestamp.
- Check whether failures correlate with a deployment, configuration
  change, or dependency outage.
- Check request rate and the HTTP 5xx metric in Grafana.
- Avoid retrying write operations blindly; requests may not be safe to
  replay.

### Phase C — Recovery

The responder's default permissions are read-only. It may collect
approved health, metric, and log evidence and recommend a recovery action.

Potential disruptive operations, including restarting services, changing
traffic, changing configuration, applying migrations, deploying code,
or modifying production data, require explicit human authorization and
must follow the relevant environment's release and recovery procedures.

After an authorized recovery:
1. Recheck application health.
2. Verify that the Prometheus backend target is up.
3. Confirm that the alert returns to `inactive` after its expression
   no longer matches and Prometheus reevaluates the rule.
4. Run an appropriate smoke test.
5. Record the action, approval, verification evidence, and remaining risks.

If recovery is uncertain, the alert recurs, or the action exceeds the
approved scope, stop and escalate to a human operator.

### Phase D — Incident record

Record:
- Alert name and timestamps.
- Affected component and observed symptoms.
- Relevant sanitized logs and metric observations.
- Diagnosis and confidence level.
- Actions taken, including who authorized disruptive actions.
- Health and smoke-test results after recovery.
- Escalation details and follow-up work.

## 9. Responder Safety Boundaries

A standalone incident-responder service is implemented and integrated
with the local Alertmanager webhook. It performs bounded, read-only
diagnostics and writes sanitized JSONL audit events. It does not execute
shell commands, change service state, or perform automatic remediation.
Escalation is recorded for human review; external paging is not configured.

**Webhook trust limitation:** webhook callers are not authenticated at the
application layer. The responder has no published host port, but other
services on its Compose network may reach it. Use only in a trusted local
environment; add webhook authentication before broader deployment.

The implementation must enforce these boundaries in code:

- Allowlist diagnostic endpoints and permitted data sources.
- Do not expose unrestricted shell execution.
- Do not provide general repository write, secret-reading, or production
  deployment privileges.
- Do not automatically execute destructive or disruptive remediation.
- Require human approval for actions that change service state.
- Limit log access and redact credentials and private content.
- Bound request size, alert count, accepted alert names, and diagnostic
  sources; do not execute alert-supplied commands or URLs.
- Stop and flag escalation when diagnostics fail or evidence is insufficient.
- Audit the alert, diagnostic result, escalation status, and whether
  remediation occurred. This version never executes remediation.

Instruction prompts alone are not a security control. Permissions and
action restrictions must be enforced by the runtime.
