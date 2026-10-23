#!/usr/bin/env bash
set -euo pipefail

for tool in docker curl java node python3; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "Missing required tool: $tool" >&2
    exit 1
  }
done

if [ ! -f .env ]; then
  echo "Missing .env. Copy .env.example to .env and configure required values." >&2
  exit 1
fi

python3 - <<'PY'
from pathlib import Path
import sys

required = ("JWT_SECRET", "GITHUB_OWNER", "GITHUB_REPOSITORY", "GITHUB_CONTENT_PATH")
values = {}

for line in Path(".env").read_text().splitlines():
    line = line.strip()
    if line and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip("'\"")

invalid = []
for key in required:
    value = values.get(key, "")
    if not value or value.lower().startswith(("replace-with-", "your-")):
        invalid.append(key)

if invalid:
    print("Invalid or placeholder environment values: " + ", ".join(invalid), file=sys.stderr)
    sys.exit(1)

print("Local environment validation passed.")
PY
