#!/bin/bash
# Triggers one call generation, waits for the pipeline to finish, then prints the full
# log trace (filtered by callId) and the contents of all three generated artifacts.
# Assumes all 4 services are already running (see start-all.sh / status.sh).
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$REPO_ROOT/logs"
MEDIA_STORE="$HOME/call-insights-pipeline/media-store"

echo "Triggering call generation..."
RESPONSE=$(curl -s -X POST http://localhost:8081/api/calls/generate)
echo "Response: $RESPONSE"

CALL_ID=$(echo "$RESPONSE" | grep -o '"callId":"[^"]*"' | cut -d'"' -f4)
if [ -z "$CALL_ID" ]; then
  echo
  echo "Could not extract callId from the response - is call-generator running? (run scripts/status.sh)"
  exit 1
fi
echo "callId: $CALL_ID"

CALL_DIR="$MEDIA_STORE/$CALL_ID"

echo
echo "Waiting for the pipeline to complete (transcript -> summary + evaluation)..."
DONE=false
for i in $(seq 1 40); do
  if [ -f "$CALL_DIR/summary_v1.json" ] && [ -f "$CALL_DIR/evaluation_v1.json" ]; then
    DONE=true
    break
  fi
  sleep 2
done

if [ "$DONE" = true ]; then
  echo "Pipeline complete."
else
  echo "Timed out waiting for completion - showing whatever is available so far."
fi

echo
echo "=== Log trace for callId=$CALL_ID ==="
grep "$CALL_ID" "$LOG_DIR"/*.log 2>/dev/null

echo
echo "=== transcript_v1.json ==="
cat "$CALL_DIR/transcript_v1.json" 2>/dev/null || echo "(not found yet)"
echo
echo "=== summary_v1.json ==="
cat "$CALL_DIR/summary_v1.json" 2>/dev/null || echo "(not found yet)"
echo
echo "=== evaluation_v1.json ==="
cat "$CALL_DIR/evaluation_v1.json" 2>/dev/null || echo "(not found yet)"

echo
echo "=== MongoDB catalog (catalog-service) ==="
if command -v mongosh >/dev/null 2>&1; then
  mongosh --quiet mongodb://localhost:27017/call-insights --eval "
    print('-- calls --'); printjson(db.calls.findOne({_id: '$CALL_ID'}));
    print('-- artifacts --'); db.artifacts.find({callId: '$CALL_ID'}).forEach(printjson);
    print('-- audit_log --'); db.audit_log.find({callId: '$CALL_ID'}).forEach(printjson);
  "
else
  echo "mongosh not found on PATH - skipping catalog check (see mongo-express at http://localhost:8091)"
fi
