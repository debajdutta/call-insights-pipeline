#!/bin/bash
# Start/stop/restart the 6 backend services only - does NOT touch Docker infra or the
# frontend dev server. Use this for a fast edit-rebuild-relaunch loop while Kafka/Mongo
# stay up and keep their data; use start-all.sh / stop-all.sh for the full stack.
#
# Usage:
#   scripts/services.sh start|stop|restart [service-name ...]
#   With no service names, applies to all 6. Examples:
#     scripts/services.sh restart gateway-service
#     scripts/services.sh restart catalog-service gateway-service
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$REPO_ROOT/logs"
mkdir -p "$LOG_DIR"

ALL_SERVICES="call-generator transcription-service summary-service evaluation-service catalog-service gateway-service"

if ! command -v mvn >/dev/null 2>&1; then
  export PATH="/opt/maven/bin:$PATH"
fi
if ! command -v java >/dev/null 2>&1; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

usage() {
  echo "Usage: $0 {start|stop|restart} [service-name ...]"
  echo "  Services: $ALL_SERVICES"
  exit 1
}

stop_service() {
  local name="$1"
  local pid
  pid=$(pgrep -f "${name}-0.1.0-SNAPSHOT.jar" || true)
  if [ -z "$pid" ]; then
    echo "[$name] not running."
    return
  fi
  echo "[$name] stopping (pid $pid)..."
  kill $pid
  for i in $(seq 1 10); do
    pgrep -f "${name}-0.1.0-SNAPSHOT.jar" >/dev/null 2>&1 || break
    sleep 1
  done
}

start_service() {
  local name="$1"
  local module_dir="$REPO_ROOT/$name"
  local jar="target/${name}-0.1.0-SNAPSHOT.jar"
  local log_file="$LOG_DIR/${name}.log"

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

ACTION="${1:-}"
[ -n "$ACTION" ] || usage
shift
TARGETS="${*:-$ALL_SERVICES}"

case "$ACTION" in
  start)
    if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
      echo "WARNING: ANTHROPIC_API_KEY is not set in this shell - transcription-service and"
      echo "         summary-service will start but real generation will fail."
      echo
    fi
    for name in $TARGETS; do start_service "$name"; done
    ;;
  stop)
    for name in $TARGETS; do stop_service "$name"; done
    ;;
  restart)
    for name in $TARGETS; do stop_service "$name"; done
    for name in $TARGETS; do start_service "$name"; done
    ;;
  *)
    usage
    ;;
esac
