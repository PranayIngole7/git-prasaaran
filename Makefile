.PHONY: test-all test-backend test-frontend test-mcp test-hooks test-responder dev-up dev-down dev-logs check-local-env

# Validate required local tools and configuration before starting services.
check-local-env:
	@command -v docker >/dev/null 2>&1 || { echo "Missing docker"; exit 1; }
	@command -v curl >/dev/null 2>&1 || { echo "Missing curl"; exit 1; }
	@command -v java >/dev/null 2>&1 || { echo "Missing Java 21"; exit 1; }
	@command -v node >/dev/null 2>&1 || { echo "Missing Node.js 22"; exit 1; }
	@command -v python3 >/dev/null 2>&1 || { echo "Missing Python 3"; exit 1; }
	@test -f .env || { echo "Missing .env. Copy .env.example to .env and fill required values."; exit 1; }
	@python3 -c 'from pathlib import Path; import sys; required = ("JWT_SECRET", "GITHUB_OWNER", "GITHUB_REPOSITORY", "GITHUB_CONTENT_PATH"); values = dict(line.split("=", 1) for line in Path(".env").read_text().splitlines() if "=" in line and not line.lstrip().startswith("#")); missing = [key for key in required if not values.get(key, "").strip() or values[key].strip().lower().startswith(("replace-with-", "your-"))]; print("Invalid or missing required .env values: " + ", ".join(missing)) if missing else None; sys.exit(bool(missing))'

# Run all automated test suites.
test-all: test-backend test-frontend test-mcp test-hooks test-responder

test-backend:
	cd backend && ./mvnw --batch-mode test

test-frontend:
	npm test --prefix frontend

test-mcp:
	npm test --prefix mcp-server

test-hooks:
	node --test agent-hooks/tests/validate-markdown.test.mjs

test-responder:
	PYTHONPATH=incident-responder python3 -m unittest discover -s incident-responder/tests -p 'test_*.py' -v

dev-up: check-local-env
	docker compose --env-file .env up -d --wait --wait-timeout 120 postgres redis backend frontend prometheus alertmanager incident-responder grafana
	@curl --fail --silent --show-error --retry 10 --retry-connrefused --retry-delay 2 http://localhost:8081/actuator/health >/dev/null || { echo "Backend health check failed."; exit 1; }
	@curl --fail --silent --show-error --retry 10 --retry-connrefused --retry-delay 2 http://localhost:8082/ >/dev/null || { echo "Frontend health check failed."; exit 1; }
	@echo "Development stack started and health checks passed."
	@echo "Backend: http://localhost:8081 (management) / http://localhost:8080 (API)"
	@echo "Frontend: http://localhost:8082"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3000"
	@echo "Alertmanager: http://localhost:9093"

dev-down:
	docker compose --env-file .env down

dev-logs:
	docker compose --env-file .env logs -f --tail=200 postgres redis backend frontend prometheus alertmanager incident-responder grafana
