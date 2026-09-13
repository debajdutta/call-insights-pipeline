#!/bin/bash
# Start/stop/restart the frontend dev server (`ng serve`) only - does NOT touch Docker
# infra or the backend services. Rarely needed on its own since `ng serve` hot-reloads on
# file changes, but useful after switching branches, after `npm install`-ing new deps, or
# if the dev server has hung/crashed.
#
# Usage: scripts/frontend.sh start|stop|restart
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"
LOG_FILE="$REPO_ROOT/logs/frontend.log"
mkdir -p "$REPO_ROOT/logs"

usage() {
  echo "Usage: $0 {start|stop|restart}"
  exit 1
}

stop_frontend() {
  local pids
  pids=$(pgrep -f "ng serve" || true)
  if [ -z "$pids" ]; then
    echo "[frontend] not running."
    return
  fi
  # pgrep can match both the `npm exec` wrapper and the actual `ng serve` process it spawns.
  echo "[frontend] stopping (pid(s) $(echo "$pids" | tr '\n' ' ' | sed 's/ *$//'))..."
  echo "$pids" | xargs kill 2>/dev/null || true
  for i in $(seq 1 10); do
    pgrep -f "ng serve" >/dev/null 2>&1 || break
    sleep 1
  done
}

start_frontend() {
  if ! command -v npm >/dev/null 2>&1; then
    echo "[frontend] npm not found on PATH - install Node.js to enable this"
    return 1
  fi

  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "[frontend] installing dependencies (first run)..."
    if ! (cd "$FRONTEND_DIR" && npm install); then
      echo "[frontend] npm install FAILED - skipping start"
      return 1
    fi
  fi

  echo "[frontend] starting (logging to $LOG_FILE)..."
  rm -f "$LOG_FILE"
  (cd "$FRONTEND_DIR" && nohup npx ng serve --port 4200 > "$LOG_FILE" 2>&1 &)

  for i in $(seq 1 30); do
    if grep -q "Local:" "$LOG_FILE" 2>/dev/null; then
      echo "[frontend] started."
      return 0
    fi
    sleep 2
  done
  echo "[frontend] did not report ready within timeout - see $LOG_FILE"
  return 1
}

ACTION="${1:-}"
case "$ACTION" in
  start)
    start_frontend
    ;;
  stop)
    stop_frontend
    ;;
  restart)
    stop_frontend
    start_frontend
    ;;
  *)
    usage
    ;;
esac
