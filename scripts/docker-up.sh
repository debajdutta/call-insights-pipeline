#!/bin/bash
# Builds and starts the whole system in Docker (infra, six services, frontend) from the root
# docker-compose.yml. Compose cannot warn about a missing key itself, so this wrapper does.
# Run it from a shell where ANTHROPIC_API_KEY is already exported (session-scoped, not a
# shell profile); compose passes it through to transcription-service and summary-service.
# Stop the script workflow first (scripts/stop-all.sh): the two cannot run at the same time.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi

key_warning() {
  [ -n "${ANTHROPIC_API_KEY:-}" ] && return 0
  echo "************************************************************************"
  echo "  ANTHROPIC_API_KEY is not set in this shell."
  echo "  transcription-service and summary-service need it; without it their"
  echo "  LLM calls fail and the pipeline stops after call generation."
  echo "  Fix, in this terminal (session only, not a shell profile):"
  echo "    export ANTHROPIC_API_KEY=<your key>"
  echo "    docker compose up -d transcription-service summary-service"
  echo "************************************************************************"
  echo
}
key_warning

(cd "$REPO_ROOT" && docker compose up -d --build)
status=$?

echo
key_warning
exit $status
