# Observability validation

Date: 2026-10-23

## Configuration reviewed
- `monitoring/prometheus.yml`
- `monitoring/alerts.yml`
- `monitoring/alertmanager.yml`
- Backend Actuator configuration
- `docker-compose.yml`

## Live local checks completed
- `http://localhost:8081/actuator/health` returned HTTP 200 with status `UP`.
- `http://localhost:8081/actuator/prometheus` returned HTTP 200 and exposed application and JVM metrics.
- Prometheus query `up{job="git-prasaaran-backend"}` returned `1` for `backend:8081`.
- Alertmanager readiness check succeeded.
- Both configured Prometheus alert rules reported healthy; both were inactive during the check.
- A controlled synthetic alert-to-responder test is documented in `alert-pipeline-validation.md`.

## Limitations
These checks verify local health, metrics exposure, scraping, and alert configuration.
They do not demonstrate trace-ID correlation across a complete user journey, metrics,
and structured logs. End-to-end distributed-trace validation remains unverified.
