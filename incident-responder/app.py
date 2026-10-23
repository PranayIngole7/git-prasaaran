"""Bounded Prometheus Alertmanager webhook receiver.

Only known alert names and fixed, read-only Prometheus endpoints are allowed.
No shell commands, automatic remediation, or arbitrary URLs are accepted.
"""

import base64
import hashlib
import hmac
import json
import os
import re
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen

ALLOWED_ALERTS = {
    "BackendTargetDown",
    "BackendHighServerErrorRate",
}
ALLOWED_STATUSES = {"firing", "resolved"}
MAX_BODY_BYTES = 1_048_576
MAX_ALERTS = 100
ALERT_WEBHOOK_SECRET = os.environ.get("ALERT_WEBHOOK_SECRET", "dev-local-only-secret")
ALERTMANAGER_BASIC_USERNAME = os.environ.get("ALERTMANAGER_WEBHOOK_USERNAME", "alertmanager")
ALERTMANAGER_BASIC_PASSWORD = os.environ.get("ALERTMANAGER_WEBHOOK_PASSWORD", "dev-alertmanager-secret")
ALERT_SIGNATURE_HEADER_NAMES = (
    "X-Signature",
    "X-Webhook-Signature",
    "X-Alertmanager-Signature",
)
ALERT_DEDUP_TTL_SECONDS = int(os.environ.get("ALERT_DEDUP_TTL_SECONDS", "300"))
SAFE_ACTION_REGISTRY = {
    "BackendTargetDown": ["diagnostic_check", "escalate_human_review"],
    "BackendHighServerErrorRate": ["diagnostic_check", "escalate_human_review"],
}
APPROVAL_REQUIRED_ACTIONS = {"escalate_human_review"}
ACTION_TIMEOUT_SECONDS = {
    "diagnostic_check": 3,
    "escalate_human_review": 2,
}
ACTION_MAX_RETRIES = {
    "diagnostic_check": 2,
    "escalate_human_review": 0,
}
ACTION_COOLDOWN_SECONDS = {
    "diagnostic_check": 90,
    "escalate_human_review": 300,
}

PROMETHEUS_URL = os.environ.get(
    "PROMETHEUS_URL", "http://localhost:9090"
).rstrip("/")
AUDIT_LOG_PATH = Path(
    os.environ.get("AUDIT_LOG_PATH", "/tmp/incident-responder-audit.jsonl")
)

_audit_lock = threading.Lock()
_seen_incidents = {}


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def safe_text(value, limit=240):
    """Keep audit strings short and free of control characters."""
    if not isinstance(value, str):
        return ""
    value = re.sub(r"[\x00-\x1f\x7f]", " ", value)
    return value[:limit]


def alert_signature_for_body(body: bytes, secret: str = ALERT_WEBHOOK_SECRET) -> str:
    """Return a signed webhook digest that is safe to pass as a request header."""
    digest = hmac.new(secret.encode("utf-8"), body, hashlib.sha256).hexdigest()
    return f"sha256={digest}"


def verify_alert_signature(headers, body: bytes, secret: str = ALERT_WEBHOOK_SECRET):
    """Validate a constant-time HMAC signature for Alertmanager webhooks."""
    if not isinstance(body, (bytes, bytearray)):
        raise ValueError("Request body must be bytes")

    provided = ""
    for header_name in ALERT_SIGNATURE_HEADER_NAMES:
        header_value = headers.get(header_name)
        if isinstance(header_value, str):
            provided = header_value.strip()
            break
    if not provided:
        raise ValueError("Missing alert signature")

    expected = alert_signature_for_body(bytes(body), secret)
    if not hmac.compare_digest(expected, provided):
        raise ValueError("Invalid alert signature")
    return True


def validate_basic_auth(headers):
    """Accept only the configured Alertmanager credentials for the authenticated boundary."""
    auth_header = headers.get("Authorization")
    if not isinstance(auth_header, str) or not auth_header.startswith("Basic "):
        raise ValueError("Missing or invalid basic auth")
    token = auth_header.split(" ", 1)[1]
    expected = base64.b64encode(
        f"{ALERTMANAGER_BASIC_USERNAME}:{ALERTMANAGER_BASIC_PASSWORD}".encode("utf-8")
    ).decode("ascii")
    if not hmac.compare_digest(token, expected):
        raise ValueError("Unauthorized Alertmanager client")
    return True


def verify_alert_auth(headers, body: bytes, secret: str = ALERT_WEBHOOK_SECRET):
    """Accept either the Alertmanager basic-auth boundary or a compatible HMAC signature."""
    auth_header = headers.get("Authorization")
    has_signature = any(
        isinstance(headers.get(name), str) and headers.get(name).strip()
        for name in ALERT_SIGNATURE_HEADER_NAMES
    )

    if isinstance(auth_header, str) and auth_header.startswith("Basic "):
        validate_basic_auth(headers)
        if has_signature:
            verify_alert_signature(headers, body, secret)
        return True

    if has_signature:
        verify_alert_signature(headers, body, secret)
        return True

    raise ValueError("Missing alert authentication")


def alert_fingerprint(alert):
    """Return a deduplication key for a firing/resolved alert event."""
    if not isinstance(alert, dict):
        return None
    labels = alert.get("labels", {})
    if not isinstance(labels, dict):
        return None
    name = labels.get("alertname")
    service = labels.get("service")
    summary = ""
    annotations = alert.get("annotations", {})
    if isinstance(annotations, dict):
        summary = annotations.get("summary", "")
    return (
        safe_text(str(name), 64),
        safe_text(str(alert.get("status", "")), 32),
        safe_text(str(service), 64),
        safe_text(str(summary), 240),
    )


def get_json_from_fixed_endpoint(path):
    """Query only hard-coded, read-only Prometheus API endpoints."""
    if path not in {"/api/v1/targets", "/api/v1/rules"}:
        raise ValueError("Diagnostic endpoint is not allowlisted")

    request_url = PROMETHEUS_URL + path
    try:
        with urlopen(request_url, timeout=2) as response:
            if response.status != 200:
                raise URLError("Prometheus returned a non-200 response")
            result = json.loads(response.read(2_000_001))
            if not isinstance(result, dict):
                raise ValueError("Unexpected Prometheus response")
            return result
    except (OSError, URLError, TimeoutError, ValueError, json.JSONDecodeError):
        return None


def collect_diagnostics():
    """Collect a minimal, sanitized snapshot; never run system commands."""
    targets_response = get_json_from_fixed_endpoint("/api/v1/targets")
    rules_response = get_json_from_fixed_endpoint("/api/v1/rules")

    targets = []
    if targets_response:
        data = targets_response.get("data", {})
        for item in data.get("activeTargets", []):
            labels = item.get("labels", {})
            if labels.get("job") == "git-prasaaran-backend":
                targets.append({
                    "job": "git-prasaaran-backend",
                    "health": safe_text(item.get("health"), 32),
                })

    rules = []
    if rules_response:
        for group in rules_response.get("data", {}).get("groups", []):
            for rule in group.get("rules", []):
                name = rule.get("name")
                if name in ALLOWED_ALERTS:
                    rules.append({
                        "name": name,
                        "health": safe_text(rule.get("health"), 32),
                        "state": safe_text(rule.get("state"), 32),
                    })

    return {
        "prometheus_available": bool(targets_response and rules_response),
        "backend_targets": targets,
        "known_alert_rules": rules,
    }


def verify_backend_health(base_url=None, timeout=2):
    """Perform a bounded, read-only health check against the local backend."""
    candidates = []
    if base_url:
        candidates.append(base_url.rstrip("/"))
    candidates.extend([
        "http://backend:8081/actuator/health",
        "http://localhost:8081/actuator/health",
        "http://localhost:8080/api/v1/health",
    ])

    for candidate in candidates:
        try:
            with urlopen(candidate, timeout=timeout) as response:
                if response.status != 200:
                    continue
                payload = json.loads(response.read(1024).decode("utf-8"))
                status = payload.get("status") if isinstance(payload, dict) else None
                if status in {"UP", "ok"}:
                    return True
        except Exception:
            continue
    raise RuntimeError("Backend health verification failed")


def get_allowed_actions(alert_name):
    """Return the allowlisted remediation actions for a supported alert."""
    if alert_name not in SAFE_ACTION_REGISTRY:
        return []
    return list(SAFE_ACTION_REGISTRY[alert_name])


def execute_allowed_action(alert_name, diagnostics, recovery_check=None):
    """Run only a bounded, explicitly allowlisted action for a known alert."""
    actions = get_allowed_actions(alert_name)
    if not actions:
        raise ValueError("Alert name is not allowlisted")

    action = actions[0]
    approval_required = action in APPROVAL_REQUIRED_ACTIONS
    result = {
        "action": action,
        "approval_required": approval_required,
        "remediation_executed": False,
        "incident_state": "investigating",
        "verification": {"status": "not_run", "verified": False},
    }

    if action == "diagnostic_check":
        try:
            if diagnostics.get("prometheus_available") is False:
                raise RuntimeError("Prometheus diagnostics unavailable")

            targets = diagnostics.get("backend_targets", [])
            healthy_targets = [
                target for target in targets
                if isinstance(target, dict) and target.get("job") == "git-prasaaran-backend"
            ]

            if not healthy_targets or any(
                target.get("health") not in {"up", "healthy"}
                for target in healthy_targets
            ):
                if recovery_check is None:
                    recovery_check = verify_backend_health
                passed = bool(recovery_check())
            else:
                passed = True

            result["verification"] = {
                "status": "passed" if passed else "failed",
                "verified": passed,
            }
            result["incident_state"] = "recovered" if passed else "failed"
            result["remediation_executed"] = False
            result["escalation_required"] = not passed
        except Exception as exc:
            result["verification"] = {"status": "failed", "verified": False}
            result["incident_state"] = "escalated"
            result["escalation_required"] = True
            result["reason"] = safe_text(str(exc), 120)
        return result

    if action == "escalate_human_review":
        result["incident_state"] = "awaiting_approval"
        result["escalation_required"] = True
        result["reason"] = "Manual approval required before any disruptive action"
        return result

    raise ValueError("Unsupported remediation action")


def process_alertmanager_payload(payload, diagnostics_provider=collect_diagnostics):
    """Validate an Alertmanager webhook and return safe audit events."""
    if not isinstance(payload, dict):
        raise ValueError("Payload must be a JSON object")

    alerts = payload.get("alerts")
    if not isinstance(alerts, list) or not alerts or len(alerts) > MAX_ALERTS:
        raise ValueError("Payload must contain between 1 and 100 alerts")

    events = []
    for alert in alerts:
        if not isinstance(alert, dict):
            raise ValueError("Each alert must be a JSON object")

        labels = alert.get("labels")
        if not isinstance(labels, dict):
            raise ValueError("Alert labels are required")

        name = labels.get("alertname")
        status = alert.get("status")

        if not isinstance(name, str) or name not in ALLOWED_ALERTS:
            raise ValueError("Alert name is not allowlisted")
        if not isinstance(status, str) or status not in ALLOWED_STATUSES:
            raise ValueError("Alert status is not supported")

        summary = safe_text(
            alert.get("annotations", {}).get("summary", "")
            if isinstance(alert.get("annotations", {}), dict)
            else ""
        )
        event = {
            "timestamp": utc_now(),
            "alertname": name,
            "status": status,
            "severity": safe_text(labels.get("severity"), 32),
            "service": safe_text(labels.get("service"), 64),
            "summary": summary,
            "action": "human_review_required",
            "remediation_executed": False,
            "deduplicated": False,
            "incident_state": "received",
            "approval_required": False,
        }

        fingerprint = alert_fingerprint(alert)
        if fingerprint is not None:
            now = time.time()
            previous = _seen_incidents.get(fingerprint)
            if previous is not None and (now - previous) < ALERT_DEDUP_TTL_SECONDS:
                event["deduplicated"] = True
                event["incident_state"] = "duplicate"
                event["action"] = "duplicate_rejected"
                event["diagnostics"] = {"collection_skipped": True}
                event["escalation_required"] = False
                events.append(event)
                continue
            _seen_incidents[fingerprint] = now

        if status == "firing":
            event["incident_state"] = "authenticated"
            try:
                diagnostics = diagnostics_provider()
                if not isinstance(diagnostics, dict):
                    raise ValueError("Invalid diagnostic result")
                event["diagnostics"] = diagnostics
                event["escalation_required"] = (
                    not diagnostics.get("prometheus_available", False)
                    or any(
                        target.get("health") != "up"
                        for target in diagnostics.get("backend_targets", [])
                    )
                )
                action_result = execute_allowed_action(
                    name,
                    diagnostics,
                    recovery_check=lambda: verify_backend_health(),
                )
                event["action"] = action_result["action"]
                event["approval_required"] = action_result.get("approval_required", False)
                event["remediation_executed"] = action_result.get("remediation_executed", False)
                event["incident_state"] = action_result["incident_state"]
                event["verification"] = action_result["verification"]
                event["escalation_required"] = bool(action_result.get("escalation_required", event["escalation_required"]))
                if "reason" in action_result:
                    event["reason"] = action_result["reason"]
            except Exception:
                event["diagnostics"] = {"prometheus_available": False}
                event["incident_state"] = "escalated"
                event["action"] = "escalate_human_review"
                event["approval_required"] = True
                event["escalation_required"] = True
        else:
            event["diagnostics"] = {"collection_skipped": True}
            event["escalation_required"] = False
            event["incident_state"] = "resolved"
            event["action"] = "resolved"

        events.append(event)

    return events


def append_audit_event(event):
    """Append one JSON event without logging raw webhook bodies."""
    AUDIT_LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
    line = json.dumps(event, sort_keys=True, separators=(",", ":"))
    with _audit_lock:
        with AUDIT_LOG_PATH.open("a", encoding="utf-8") as stream:
            stream.write(line + "\n")


class ResponderHandler(BaseHTTPRequestHandler):
    server_version = "GitPrasaaranResponder/1.0"

    def do_GET(self):
        if self.path != "/health":
            self.send_error(404)
            return

        body = b'{"status":"ok","mode":"read-only"}'
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        if self.path != "/api/v1/alerts":
            self.send_error(404)
            return

        content_type = self.headers.get("Content-Type", "")
        if not content_type.lower().startswith("application/json"):
            self.send_error(415, "Content-Type must be application/json")
            return

        try:
            length = int(self.headers.get("Content-Length", "-1"))
        except ValueError:
            self.send_error(400, "Invalid Content-Length")
            return

        if length < 1 or length > MAX_BODY_BYTES:
            self.send_error(413, "Invalid or oversized request body")
            return

        raw_body = self.rfile.read(length)
        try:
            verify_alert_auth(self.headers, raw_body)
        except ValueError as exc:
            self.send_error(401, safe_text(str(exc), 120))
            return

        try:
            payload = json.loads(raw_body.decode("utf-8"))
            events = process_alertmanager_payload(payload)
            for event in events:
                append_audit_event(event)
        except (ValueError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            self.send_error(400, safe_text(str(exc), 120))
            return
        except OSError:
            self.send_error(500, "Audit record could not be written")
            return

        body = json.dumps({
            "accepted": len(events),
            "remediation_executed": False,
            "duplicate_alerts": sum(1 for event in events if event.get("deduplicated")),
        }).encode("utf-8")
        self.send_response(202)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        # Avoid writing webhook payloads or untrusted alert text to access logs.
        return


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8080), ResponderHandler)
    print("Incident responder listening on port 8080 (read-only mode)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
