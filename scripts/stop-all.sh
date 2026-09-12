#!/bin/bash
# Stops all 4 backend service processes. Leaves Docker infra (Kafka/Mongo/UIs) running,
# since that's meant to stay up long-term - re-run infra/docker-compose.yml yourself if
# you also want that down.
set -uo pipefail

echo "Stopping backend services (Docker infra left running)..."
for name in call-generator transcription-service summary-service evaluation-service; do
  pid=$(pgrep -f "${name}-0.1.0-SNAPSHOT.jar" || true)
  if [ -n "$pid" ]; then
    echo "[$name] stopping (pid $pid)..."
    kill $pid
  else
    echo "[$name] not running."
  fi
done

sleep 2
echo
echo "Remaining matching processes (should be none):"
pgrep -fl "call-generator-0.1.0-SNAPSHOT.jar|transcription-service-0.1.0-SNAPSHOT.jar|summary-service-0.1.0-SNAPSHOT.jar|evaluation-service-0.1.0-SNAPSHOT.jar" || echo "  (none)"
