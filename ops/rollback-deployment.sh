#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <known-good-version>" >&2
  exit 2
fi

VERSION="$1"

if [[ ! "$VERSION" =~ ^[A-Za-z0-9][A-Za-z0-9._:+-]*$ ]]; then
  echo "Invalid version format. Use a release tag or immutable version identifier." >&2
  exit 2
fi

if [ -z "${PROD_HEALTH_URL:-}" ]; then
  echo "PROD_HEALTH_URL must be configured for production health verification." >&2
  exit 1
fi

if [[ ! "$PROD_HEALTH_URL" =~ ^https?:// ]]; then
  echo "PROD_HEALTH_URL must be an HTTP or HTTPS URL." >&2
  exit 1
fi

echo "Rollback preflight passed for target: $VERSION"
echo "Health endpoint configuration is present."
echo "No deployment was changed; no rollback was executed."
echo "Configure the hosting platform's deployment command before using this as an operational rollback."
