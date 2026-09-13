#!/bin/bash
# Full teardown: stops all 6 backend services, the frontend dev server (if running via
# `ng serve`), and brings down Docker infra (Kafka/Mongo/UIs) via `docker compose down`.
# Use this when you're done for the day. For a fast restart loop that leaves infra (and
# its data) alone, use scripts/services.sh instead.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi

echo "=== Backend services ==="
"$REPO_ROOT/scripts/services.sh" stop

echo
echo "=== Frontend dev server ==="
"$REPO_ROOT/scripts/frontend.sh" stop

sleep 2
echo
echo "Remaining matching processes (should be none):"
pgrep -fl "call-generator-0.1.0-SNAPSHOT.jar|transcription-service-0.1.0-SNAPSHOT.jar|summary-service-0.1.0-SNAPSHOT.jar|evaluation-service-0.1.0-SNAPSHOT.jar|catalog-service-0.1.0-SNAPSHOT.jar|gateway-service-0.1.0-SNAPSHOT.jar|ng serve" || echo "  (none)"

echo
echo "=== Docker infra ==="
(cd "$REPO_ROOT/infra" && docker compose down)
