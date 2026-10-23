import base64
import hashlib
import hmac
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import app


def make_alert(name="BackendTargetDown", status="firing"):
    return {
        "status": status,
        "labels": {
            "alertname": name,
            "severity": "critical",
            "service": "git-prasaaran",
        },
        "annotations": {"summary": "Backend metrics target unavailable"},
    }


def sign_payload(payload, secret=app.ALERT_WEBHOOK_SECRET):
    body = json.dumps(payload, separators=(",", ":"), sort_keys=True).encode("utf-8")
    digest = hmac.new(secret.encode("utf-8"), body, hashlib.sha256).hexdigest()
    return {"X-Signature": f"sha256={digest}"}


def basic_auth_headers(username=app.ALERTMANAGER_BASIC_USERNAME, password=app.ALERTMANAGER_BASIC_PASSWORD):
    token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
    return {"Authorization": f"Basic {token}"}


class ResponderTests(unittest.TestCase):
    def setUp(self):
        app._seen_incidents.clear()

    def test_accepts_allowlisted_firing_alert_and_collects_diagnostics(self):
        diagnostics = {
            "prometheus_available": True,
            "backend_targets": [{"job": "git-prasaaran-backend", "health": "up"}],
            "known_alert_rules": [],
        }
        events = app.process_alertmanager_payload(
            {"alerts": [make_alert()]},
            diagnostics_provider=lambda: diagnostics,
        )
        self.assertEqual(len(events), 1)
        self.assertEqual(events[0]["alertname"], "BackendTargetDown")
        self.assertFalse(events[0]["remediation_executed"])
        self.assertFalse(events[0]["escalation_required"])

    def test_resolved_alert_skips_diagnostics(self):
        def should_not_run():
            self.fail("Resolved alert should not collect diagnostics")

        events = app.process_alertmanager_payload(
            {"alerts": [make_alert(status="resolved")]},
            diagnostics_provider=should_not_run,
        )
        self.assertTrue(events[0]["diagnostics"]["collection_skipped"])
        self.assertFalse(events[0]["remediation_executed"])

    def test_rejects_non_string_alert_name(self):
        payload = {"alerts": [{
            "status": "firing",
            "labels": {"alertname": ["BackendTargetDown"]},
        }]}
        with self.assertRaisesRegex(ValueError, "Alert name is not allowlisted"):
            app.process_alertmanager_payload(payload, diagnostics_provider=lambda: {})

    def test_rejects_non_string_alert_status(self):
        payload = {"alerts": [{
            "status": {"unexpected": "object"},
            "labels": {"alertname": "BackendTargetDown"},
        }]}
        with self.assertRaisesRegex(ValueError, "Alert status is not supported"):
            app.process_alertmanager_payload(payload, diagnostics_provider=lambda: {})

    def test_rejects_unknown_alert_name(self):
        with self.assertRaisesRegex(ValueError, "not allowlisted"):
            app.process_alertmanager_payload(
                {"alerts": [make_alert(name="RunArbitraryCommand")]}
            )

    def test_rejects_invalid_status_and_empty_alerts(self):
        with self.assertRaisesRegex(ValueError, "not supported"):
            app.process_alertmanager_payload(
                {"alerts": [make_alert(status="unknown")]}
            )
        with self.assertRaisesRegex(ValueError, "between 1 and 100"):
            app.process_alertmanager_payload({"alerts": []})

    def test_diagnostic_failure_escalates_without_remediation(self):
        def fail():
            raise RuntimeError("diagnostic unavailable")

        events = app.process_alertmanager_payload(
            {"alerts": [make_alert()]}, diagnostics_provider=fail
        )
        self.assertTrue(events[0]["escalation_required"])
        self.assertFalse(events[0]["remediation_executed"])
        self.assertNotIn("diagnostic unavailable", str(events[0]))

    def test_summary_is_sanitized_and_bounded(self):
        summary = "bad\nsummary " + ("x" * 300)
        payload = {"alerts": [make_alert()]}
        payload["alerts"][0]["annotations"]["summary"] = summary
        event = app.process_alertmanager_payload(
            payload, diagnostics_provider=lambda: {"prometheus_available": True}
        )[0]
        self.assertNotIn("\n", event["summary"])
        self.assertLessEqual(len(event["summary"]), 240)

    def test_rejects_missing_signature_for_alert_batch(self):
        with self.assertRaisesRegex(ValueError, "Missing alert signature"):
            app.verify_alert_signature({}, b'{"alerts": []}')

    def test_rejects_invalid_signature_for_alert_batch(self):
        with self.assertRaisesRegex(ValueError, "Invalid alert signature"):
            app.verify_alert_signature({"X-Signature": "sha256=deadbeef"}, b'{"alerts": []}')

    def test_duplicate_firing_alerts_are_marked_duplicate(self):
        payload = {"alerts": [make_alert()]}
        first = app.process_alertmanager_payload(
            payload,
            diagnostics_provider=lambda: {"prometheus_available": True, "backend_targets": [{"job": "git-prasaaran-backend", "health": "up"}]},
        )
        second = app.process_alertmanager_payload(
            payload,
            diagnostics_provider=lambda: {"prometheus_available": True, "backend_targets": [{"job": "git-prasaaran-backend", "health": "up"}]},
        )
        self.assertFalse(first[0]["deduplicated"])
        self.assertTrue(second[0]["deduplicated"])
        self.assertEqual(second[0]["incident_state"], "duplicate")

    def test_audit_event_is_jsonl(self):
        event = {"alertname": "BackendTargetDown", "remediation_executed": False}
        with tempfile.TemporaryDirectory() as directory:
            audit_path = Path(directory) / "audit.jsonl"
            with patch.object(app, "AUDIT_LOG_PATH", audit_path):
                app.append_audit_event(event)
            stored = json.loads(audit_path.read_text().strip())
            self.assertEqual(stored["alertname"], "BackendTargetDown")
            self.assertFalse(stored["remediation_executed"])


if __name__ == "__main__":
    unittest.main()
