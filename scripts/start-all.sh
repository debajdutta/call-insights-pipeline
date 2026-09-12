#!/bin/bash
# Brings up Docker infra (if not already running) and (re)builds + (re)launches all 5
# backend services. Idempotent: stops any existing instance of each jar before starting fresh.
# Run this from a shell where ANTHROPIC_API_KEY is already set (e.g. via ~/.zshrc) so
# transcription-service and summary-service inherit it.
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

if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "WARNING: ANTHROPIC_API_KEY is not set in this shell."
  echo "         transcription-service and summary-service will still start, but any"
  echo "         real generation will fail until the key is set (e.g. in ~/.zshrc)."
  echo
fi

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
    kill $pid 2>/dev/null || true
    for i in $(seq 1 10); do
      pgrep -f "${name}-0.1.0-SNAPSHOT.jar" >/dev/null 2>&1 || break
      sleep 1
    done
  fi

  echo "[$name] building..."
  if ! (cd "$module_dir" && mvn -q -DskipTests package); then
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

echo
echo "=== Final status ==="
"$REPO_ROOT/scripts/status.sh"
