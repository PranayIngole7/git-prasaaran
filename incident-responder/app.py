"""Bounded Prometheus Alertmanager webhook receiver.

Only known alert names and fixed, read-only Prometheus endpoints are allowed.
No shell commands, automatic remediation, or arbitrary URLs are accepted.
"""

import json
import os
import re
import threading
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

PROMETHEUS_URL = os.environ.get(
    "PROMETHEUS_URL", "http://localhost:9090"
).rstrip("/")
AUDIT_LOG_PATH = Path(
    os.environ.get("AUDIT_LOG_PATH", "/tmp/incident-responder-audit.jsonl")
)

_audit_lock = threading.Lock()


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def safe_text(value, limit=240):
    """Keep audit strings short and free of control characters."""
    if not isinstance(value, str):
        return ""
    value = re.sub(r"[\x00-\x1f\x7f]", " ", value)
    return value[:limit]


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

        event = {
            "timestamp": utc_now(),
            "alertname": name,
            "status": status,
            "severity": safe_text(labels.get("severity"), 32),
            "service": safe_text(labels.get("service"), 64),
            "summary": safe_text(
                alert.get("annotations", {}).get("summary", "")
                if isinstance(alert.get("annotations", {}), dict)
                else ""
            ),
            "action": "human_review_required",
            "remediation_executed": False,
        }

        if status == "firing":
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
            except Exception:
                # Alert receipt must not trigger unsafe fallback behavior.
                event["diagnostics"] = {"prometheus_available": False}
                event["escalation_required"] = True
        else:
            event["diagnostics"] = {"collection_skipped": True}
            event["escalation_required"] = False

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

        try:
            payload = json.loads(self.rfile.read(length))
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
