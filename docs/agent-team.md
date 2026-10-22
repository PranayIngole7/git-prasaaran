# Git-Prasaaran AI Agent Team

## Purpose

This document defines the responsibilities, collaboration process, and
security boundaries for AI-assisted development and operational response.

## Roles

### 1. Specification Agent
- Convert product requirements into testable acceptance criteria.
- Identify relevant public GitHub issues.
- Identify affected components and required tests.
- Do not implement changes before the task scope is defined.

### 2. Implementation Agent
- Read AGENTS.md and the relevant project documentation before editing.
- Implement only the approved task scope.
- Follow existing architecture and security boundaries.
- Add or update tests for changed behavior.
- Report modified files, commands executed, results, and limitations.

### 3. Verification Agent
- Compare implementation against the issue acceptance criteria.
- Review the diff for regressions, security problems, and scope violations.
- Execute relevant tests and build checks.
- Report failures honestly; never claim an unexecuted test passed.
- Recommend completion only when evidence supports it.

### 4. Operations / Responder Agent (Target Role)

The intended future role would receive authenticated operational alerts,
deduplicate incidents, consult runbooks, and help verify recovery before
escalating unresolved incidents to a human.

The current local responder accepts allowlisted Alertmanager webhooks,
collects bounded read-only Prometheus diagnostics, and records sanitized
audit events. The webhook has no application-level authentication; use it
only within a trusted local Compose environment. The responder does not
deduplicate incidents, execute remediation, or verify recovery through a
synthetic user journey. Those capabilities remain future requirements.

This role is a separate runtime responsibility. It is not the same as the
development agents or the existing documentation Q&A assistant.

### Responder Implementation and Authorization Contract

The local incident-responder runtime is implemented and connected to
Alertmanager through a webhook. It accepts allowlisted alerts, gathers
bounded read-only diagnostics, and writes sanitized audit events.
Incident recovery remains operator-led using the runbook in `ops/README.md`.

Any responder implementation must follow these constraints:

- Default to read-only diagnostics from explicitly allowlisted sources.
- Validate the alert source and reject untrusted alert payloads.
- Never turn alert text, log content, or model output into arbitrary shell
  commands.
- Do not expose general-purpose shell, repository write, secrets, or
  production deployment access.
- Require explicit human authorization before disruptive state changes.
- Enforce action limits in executable code, not only in prompts.
- Use bounded retries and stop on repeated failures.
- Verify recovery with health checks and a relevant smoke test.
- Escalate when the cause is unclear, authorization is missing, or recovery
  cannot be verified.
- Keep an audit record of the alert, diagnosis, approval, action, and result.

Local Alertmanager-to-responder webhook delivery, diagnostic collection,
and audit logging have been tested. External email/Slack paging and
autonomous remediation are not implemented and must not be claimed.

## Collaboration Process

1. The Specification Agent defines the task and acceptance criteria.
2. A public GitHub issue records the scope and expected evidence.
3. The Implementation Agent makes the scoped changes and adds tests.
4. The Verification Agent checks the changes and records test results.
5. A human reviews and approves changes before they are accepted.
6. CI executes the applicable automated checks.
7. Deployment follows the project's approved release process.
8. Operational alerts are handled by the separately scoped responder.

Every significant task should link its issue, pull request or commit,
verification results, and any known limitations.

## Permissions and Safety

- Agents receive only the minimum permissions required for their role.
- AI-generated changes require human review.
- No agent may expose credentials, tokens, or private document contents.
- Published Markdown remains sourced from Git.
- Private documents must remain owner-scoped.
- Webhook signature validation and HTML sanitization must remain enabled.
- MCP must not expose unrestricted shell or repository access.
- Production publishing and production deployment remain human-controlled.
- This responder version executes no recovery or remediation operations.
- Any future state-changing operations require explicit authorization and
  separate implementation, testing, and review.
- Diagnostic failures must be recorded and escalated for human review.
- Instructions alone are not a security boundary; enforce permissions in code.

## Completion Evidence

For each significant task, record:
- Public issue and acceptance criteria.
- Files changed.
- Tests and checks actually executed.
- Results and relevant CI run.
- Human review status.
- Remaining limitations.
