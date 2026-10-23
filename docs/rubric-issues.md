# Git-Prasaaran rubric issue drafts

These issue drafts are prepared locally because the GitHub CLI is not installed or authenticated in this environment. They describe the expected public issue content for the seven rubric workstreams and should be published by a maintainer using the repository's normal issue workflow.

## 1. Reproducible local startup and configuration

- Title: Improve local startup to a single documented command
- Problem: New contributors need a reliable local startup path without manually guessing environment variables.
- Scope: Makefile, local environment validation, documentation, Compose defaults, ports, and troubleshooting.
- Acceptance criteria:
  - `make dev-up` validates required tools and `.env` entries.
  - Docker services start in a documented order.
  - Health checks confirm backend and frontend availability.
  - `make dev-down` and `make dev-logs` are documented.
- Security/operations considerations: no secrets are printed; `.env` remains local-only.
- Related docs: `Makefile`, `ops/README.md`, `docker-compose.yml`, `.env.example`.

## 2. End-to-end observability and traceability

- Title: Add trace-linked request metrics and structured logs for a real user journey
- Problem: The application exposes metrics and logs but lacks a single traceable user journey with shared trace IDs.
- Scope: backend HTTP request instrumentation, logging configuration, frontend boundary propagation, Prometheus configuration, query guide.
- Acceptance criteria:
  - One end-to-end journey is instrumented across backend components.
  - Metrics and logs share a trace ID.
  - Prometheus and Grafana targets match configured service names and ports.
- Related implementation: `backend/src/main/resources/application.properties`, `monitoring/prometheus.yml`, backend logging config.

## 3. Alerting and responder pipeline verification

- Title: Verify alert lifecycle from Prometheus to responder and recovery
- Problem: Monitoring and responder components exist but need a controlled failure and recovery scenario that proves the pipeline works.
- Scope: Prometheus rules, Alertmanager routing, responder diagnostics, synthetic recovery verification.
- Acceptance criteria:
  - A local failure triggers a configured alert.
  - Alertmanager delivers the alert to the responder.
  - The responder records the event and escalates safely.
  - Recovery leads to a resolved or inactive alert state.
- Related files: `monitoring/alerts.yml`, `monitoring/alertmanager.yml`, `incident-responder/app.py`.

## 4. Secure responder auth, deduplication, and bounded action model

- Title: Harden the incident responder against unauthenticated or duplicate alerts
- Problem: The local responder accepts webhook payloads without authentication or duplication controls.
- Scope: HMAC signature verification, replay protection, duplicate detection, safe audit logging, approval/diagnostic-only workflow.
- Acceptance criteria:
  - Missing or invalid signatures are rejected with HTTP 401.
  - Duplicate alerts are deduplicated and recorded as such.
  - Only allowlisted alerts and fields are accepted.
  - No shell or unrestricted remediation execution is possible.
- Related files: `incident-responder/app.py`, `incident-responder/tests/test_app.py`.

## 5. CI/CD deployment, promotion, and rollback

- Title: Add development deployment, manual production promotion, and rollback workflow
- Problem: The repository has CI tests but no validated deployment or rollback path beyond manual steps.
- Scope: GitHub Actions workflows, immutable image selection, deployment health checks, rollback procedure.
- Acceptance criteria:
  - `dev-deploy.yml` validates tests and deployment prerequisites; the actual deployment command is not implemented.
  - Production promotion declares a GitHub environment; an approval gate requires repository protection rules, and artifact promotion is not implemented.
  - The rollback workflow accepts a version input and checks endpoint health, but does not restore a version or verify a completed rollback.
- Related files: `.github/workflows/ci.yml`, new deployment workflows, `ops/README.md`.

## 6. Product specification and acceptance criteria traceability

- Title: Trace product requirements to implementation and validation evidence
- Problem: The rubric needs clear mapping from product requirements to tests and evidence.
- Scope: `product-spec.md`, issue drafts, verification docs, file-level traceability matrix.
- Acceptance criteria:
  - Every major requirement maps to an implementation and test.
  - Public issue drafts or published issues are linked to evidence.
- Related docs: `product-spec.md`, `docs/evidence/*.md`.

## 7. Final rubric evidence package

- Title: Publish evidence-backed rubric validation report
- Problem: Reviewers need a single, sanitized evidence set showing what was executed and what remains unverified.
- Scope: docs/evidence directory, command log, results summary, limits.
- Acceptance criteria:
  - Validation commands and outputs are recorded.
  - Results are separated into automated tests, local integration checks, and unverified manual steps.
  - Remaining limitations are reported candidly.
