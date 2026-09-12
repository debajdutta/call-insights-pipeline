#!/bin/bash
# Readiness check: Docker infra, all 5 service actuator endpoints, ANTHROPIC_API_KEY presence.
# Read-only - makes no changes.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi

echo "=== Docker infra ==="
if command -v docker >/dev/null 2>&1; then
  (cd "$REPO_ROOT/infra" && docker compose ps --format "table {{.Name}}\t{{.Status}}")
else
  echo "docker CLI not found on PATH"
fi

check_health() {
  local name="$1" port="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" 2>/dev/null)
  if [ "$code" = "200" ]; then
    echo "  $name (:$port): UP"
  else
    echo "  $name (:$port): NOT REACHABLE (http ${code:-000})"
  fi
}

echo
echo "=== Service health ==="
check_health call-generator 8081
check_health transcription-service 8082
check_health summary-service 8083
check_health evaluation-service 8084
check_health catalog-service 8085

check_key() {
  local name="$1"
  local pid
  pid=$(pgrep -f "${name}-0.1.0-SNAPSHOT.jar" | head -1)
  if [ -z "$pid" ]; then
    echo "  $name: not running"
    return
  fi
  local present
  present=$(ps eww "$pid" | tr ' ' '\n' | grep -c "^ANTHROPIC_API_KEY=")
  if [ "$present" -ge 1 ]; then
    echo "  $name (pid $pid): key present"
  else
    echo "  $name (pid $pid): KEY MISSING"
  fi
}

echo
echo "=== ANTHROPIC_API_KEY presence (checked by name only, value never read) ==="
check_key transcription-service
check_key summary-service
