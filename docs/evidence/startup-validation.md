# Startup validation

Date: 2026-10-23

## Documented startup command

```bash
make dev-up
```
### Validation performed

- make -n dev-up inspected the planned commands without executing them.
- docker compose config validated the Compose configuration.
- The local development stack was running during subsequent health, metrics,
  Prometheus, Alertmanager, and synthetic alert-pipeline checks.

### Result

The Makefile provides `make dev-up`, `make dev-down`, and `make dev-logs`.
The startup target checks for required tools and environment keys before invoking Compose.

### Remaining limitation

A successful execution of `make dev-up` itself has not been established by the dry-run result. The later live checks demonstrate that the relevant local services were running, not that this Makefile target started them successfully from a stopped state.
