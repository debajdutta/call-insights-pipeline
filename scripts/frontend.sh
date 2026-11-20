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
FRONTEND_DIR_REAL="$(cd "$FRONTEND_DIR" && pwd -P)"
FRONTEND_PORT=4200
LOG_FILE="$REPO_ROOT/logs/frontend.log"
mkdir -p "$REPO_ROOT/logs"

usage() {
  echo "Usage: $0 {start|stop|restart}"
  exit 1
}

# PIDs of `ng serve` (and its `npm exec` wrapper) started from this repo's frontend/ only,
# so other projects' Angular dev servers are never touched.
frontend_pids() {
  local pid cwd
  for pid in $(pgrep -f "ng serve" || true); do
    cwd=$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p')
    [ "$cwd" = "$FRONTEND_DIR_REAL" ] && echo "$pid"
  done
}

stop_frontend() {
  local pids
  pids=$(frontend_pids)
  if [ -z "$pids" ]; then
    echo "[frontend] not running."
    return
  fi
  pids=$(echo $pids)
  echo "[frontend] stopping (pid(s) $pids)..."
  # Resume first: a suspended (Ctrl-Z) process ignores SIGTERM until continued
  kill -CONT $pids 2>/dev/null || true
  kill $pids 2>/dev/null || true
  for i in $(seq 1 10); do
    [ -z "$(frontend_pids)" ] && return 0
    sleep 1
  done
  echo "[frontend] did not stop within 10s - forcing (kill -9)"
  kill -9 $pids 2>/dev/null || true
  sleep 1
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

  local holder
  holder=$(lsof -nP -iTCP:$FRONTEND_PORT -sTCP:LISTEN -t 2>/dev/null | head -1)
  if [ -n "$holder" ]; then
    echo "[frontend] port $FRONTEND_PORT is already in use by pid $holder ($(ps -o command= -p "$holder" | cut -c1-80)) - not starting"
    return 1
  fi

  echo "[frontend] starting (logging to $LOG_FILE)..."
  rm -f "$LOG_FILE"
  (cd "$FRONTEND_DIR" && nohup npx ng serve --port $FRONTEND_PORT > "$LOG_FILE" 2>&1 &)

  local fail_pattern="An unhandled exception|already in use|npm ERR!|npm error"
  for i in $(seq 1 30); do
    if grep -q "Local:" "$LOG_FILE" 2>/dev/null; then
      echo "[frontend] started."
      return 0
    fi
    if grep -q -E "$fail_pattern" "$LOG_FILE" 2>/dev/null; then
      echo "[frontend] FAILED TO START:"
      grep -m1 -E "$fail_pattern" "$LOG_FILE" | sed 's/^/  /'
      return 1
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
