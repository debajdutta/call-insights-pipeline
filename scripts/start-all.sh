#!/bin/bash
# Brings up Docker infra (if not already running) and (re)builds + (re)launches all 6
# backend services plus the frontend dev server. Idempotent: stops any existing instance
# of each jar (and any running `ng serve`) before starting fresh.
# Run this from a shell where ANTHROPIC_API_KEY is already exported (session-scoped, not a
# shell profile) so transcription-service and summary-service inherit it.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$REPO_ROOT/logs"
mkdir -p "$LOG_DIR"

if ! command -v docker >/dev/null 2>&1; then
  export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi
if ! command -v mvn >/dev/null 2>&1; then
  export PATH="/opt/maven/bin:$PATH"
fi
if ! command -v java >/dev/null 2>&1; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

key_warning() {
  [ -n "${ANTHROPIC_API_KEY:-}" ] && return 0
  echo "************************************************************************"
  echo "  ANTHROPIC_API_KEY is not set in this shell."
  echo "  transcription-service and summary-service need it; without it their"
  echo "  LLM calls fail and the pipeline stops after call generation."
  echo "  Fix, in this terminal (session only, not a shell profile):"
  echo "    export ANTHROPIC_API_KEY=<your key>"
  echo "    scripts/services.sh restart transcription-service summary-service"
  echo "************************************************************************"
  echo
}
key_warning

echo "=== Docker infra ==="
(cd "$REPO_ROOT/infra" && docker compose up -d)
echo "Waiting for Kafka and MongoDB to report healthy..."
for i in $(seq 1 30); do
  kafka_health=$(docker inspect --format='{{.State.Health.Status}}' call-insights-kafka 2>/dev/null || true)
  mongo_health=$(docker inspect --format='{{.State.Health.Status}}' call-insights-mongo 2>/dev/null || true)
  if [ "$kafka_health" = "healthy" ] && [ "$mongo_health" = "healthy" ]; then
    echo "Infra healthy."
    break
  fi
  sleep 2
done

restart_service() {
  local name="$1" module_dir="$2"
  local jar="target/${name}-0.1.0-SNAPSHOT.jar"
  local log_file="$LOG_DIR/${name}.log"

  local pid
  pid=$(pgrep -f "${name}-0.1.0-SNAPSHOT.jar" || true)
  if [ -n "$pid" ]; then
    echo "[$name] stopping existing instance (pid $pid)..."
    # Resume first: a suspended (Ctrl-Z) process ignores SIGTERM until continued
    kill -CONT $pid 2>/dev/null || true
    kill $pid 2>/dev/null || true
    for i in $(seq 1 10); do
      pgrep -f "${name}-0.1.0-SNAPSHOT.jar" >/dev/null 2>&1 || break
      sleep 1
    done
    if pgrep -f "${name}-0.1.0-SNAPSHOT.jar" >/dev/null 2>&1; then
      echo "[$name] did not stop within 10s - forcing (kill -9)"
      kill -9 $pid 2>/dev/null || true
      sleep 1
    fi
  fi

  echo "[$name] building..."
  # Build through the root reactor (-am) so callinsights-common is built alongside the
  # service; a standalone module build cannot resolve it on a clean clone.
  if ! (cd "$REPO_ROOT" && mvn -q -DskipTests -pl "$name" -am package); then
    echo "[$name] BUILD FAILED - skipping start"
    return 1
  fi

  echo "[$name] starting (logging to $log_file)..."
  rm -f "$log_file"
  (cd "$module_dir" && nohup java "-Dlogging.file.name=$log_file" -jar "$jar" > /dev/null 2>&1 &)

  for i in $(seq 1 30); do
    if grep -q "Started" "$log_file" 2>/dev/null; then
      echo "[$name] started."
      return 0
    fi
    if grep -qi "APPLICATION FAILED TO START" "$log_file" 2>/dev/null; then
      echo "[$name] FAILED TO START - see $log_file"
      return 1
    fi
    sleep 2
  done
  echo "[$name] did not report ready within timeout - see $log_file"
  return 1
}

echo
echo "=== Backend services ==="
restart_service call-generator "$REPO_ROOT/call-generator"
restart_service transcription-service "$REPO_ROOT/transcription-service"
restart_service summary-service "$REPO_ROOT/summary-service"
restart_service evaluation-service "$REPO_ROOT/evaluation-service"
restart_service catalog-service "$REPO_ROOT/catalog-service"
restart_service gateway-service "$REPO_ROOT/gateway-service"

echo
echo "=== Frontend ==="
"$REPO_ROOT/scripts/frontend.sh" restart

echo
echo "=== Final status ==="
"$REPO_ROOT/scripts/status.sh"
echo
key_warning
