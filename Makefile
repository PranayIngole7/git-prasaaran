.PHONY: test-all test-backend test-frontend test-mcp test-hooks test-responder

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
