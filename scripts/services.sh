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
  # Resume first: a suspended (Ctrl-Z) process ignores SIGTERM until continued
  kill -CONT $pid 2>/dev/null || true
  kill $pid
  for i in $(seq 1 10); do
    pgrep -f "${name}-0.1.0-SNAPSHOT.jar" >/dev/null 2>&1 || return 0
    sleep 1
  done
  echo "[$name] did not stop within 10s - forcing (kill -9)"
  kill -9 $pid 2>/dev/null || true
  sleep 1
}

start_service() {
  local name="$1"
  local module_dir="$REPO_ROOT/$name"
  local jar="target/${name}-0.1.0-SNAPSHOT.jar"
  local log_file="$LOG_DIR/${name}.log"

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

# True when any target service calls the LLM and so needs the key
needs_key() {
  case " $TARGETS " in
    *" transcription-service "*|*" summary-service "*) return 0 ;;
  esac
  return 1
}

ACTION="${1:-}"
[ -n "$ACTION" ] || usage
shift
TARGETS="${*:-$ALL_SERVICES}"

case "$ACTION" in
  start)
    needs_key && key_warning
    for name in $TARGETS; do start_service "$name"; done
    needs_key && key_warning
    ;;
  stop)
    for name in $TARGETS; do stop_service "$name"; done
    ;;
  restart)
    needs_key && key_warning
    for name in $TARGETS; do stop_service "$name"; done
    for name in $TARGETS; do start_service "$name"; done
    needs_key && key_warning
    ;;
  *)
    usage
    ;;
esac
