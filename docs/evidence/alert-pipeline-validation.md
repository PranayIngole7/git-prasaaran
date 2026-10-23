# Alert pipeline validation

Date: 2026-10-23

## Configuration reviewed
- `monitoring/alerts.yml`
- `monitoring/alertmanager.yml`
- `incident-responder/app.py`
- `docker-compose.yml`

## Controlled integration test

Run ID: `manual-validation-1792766853`

- Submitted a synthetic `BackendTargetDown` alert to local Alertmanager: HTTP 200.
- Confirmed alert delivery in the responder's JSONL audit log.
- Prometheus diagnostics succeeded; Prometheus was reachable and the backend scrape target was up.
- Responder recovery verification passed and reported incident state `recovered`.
- No remediation was executed.
- Submitted the alert resolution: HTTP 200.
- Confirmed the synthetic alert was no longer in the active alert list.

The audit event recorded `diagnostic_check`, `verification.status=passed`,
`verification.verified=true`, and `remediation_executed=false`.

## Other verified behavior
- Alert names are restricted to the configured allowlist.
- Firing alerts collect diagnostics from fixed Prometheus endpoints.
- Resolved alerts skip diagnostics.
- Duplicate alerts are handled by responder deduplication logic.
- HMAC verification is enforced for webhook requests.

## Limitations
This was a controlled synthetic alert test. It did not induce a real backend outage,
demonstrate automatic repair, or validate production incident handling. The responder
uses bounded diagnostics and does not execute arbitrary shell commands.

## Controlled live failure and recovery — 2026-10-23

**Result: PASS for the local controlled scenario.**

Procedure:
1. Confirmed the local Compose backend and Prometheus were healthy.
2. Stopped the local Compose backend to induce a monitored failure.
3. Observed Prometheus fire `BackendTargetDown`.
4. Restored the backend and waited for its health endpoint to return `{"status":"UP"}`.
5. Confirmed the Prometheus backend target returned to `up`.
6. Confirmed Prometheus no longer reported an active `BackendTargetDown` alert.
7. Inspected the incident-responder JSONL audit file.

Evidence:
- Prometheus fired `BackendTargetDown` for target `backend:8081`.
- The responder audit recorded a firing event with the backend target down and escalation required.
- The responder audit subsequently recorded a `resolved` event.
- The backend returned to healthy status, and Prometheus reported the target as `up`.
- No remediation command was executed; the responder's audit records `remediation_executed: false`.

Scope and limitations:
- This was a controlled local Compose test, not a production incident.
- It demonstrates the monitored failure, responder processing, recovery notification, and monitoring recovery for this scenario.
- It does not demonstrate autonomous remediation or validate production credentials, deployment, or rollback.
