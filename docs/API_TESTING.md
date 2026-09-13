# Gateway/BFF API — curl examples

Manual test commands for `gateway-service` (port `8086`), the single entry point for the
frontend (SPEC.md FR4-FR8). Requires the full pipeline running (`scripts/start-all.sh`).

**Keep this in sync:** whenever an endpoint on `gateway-service` is added, removed, or changes
its request/response shape, update this file in the same change.

## 1. Auth

### Login (success)

```bash
curl -s -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"supervisor","password":"ChangeMe123!"}'
```

Response: `{"token": "...", "username": "supervisor", "expiresInSeconds": 28800}`. Save the token:

```bash
export TOKEN="paste-the-token-here"
```

### Login (wrong password → 401)

```bash
curl -i -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"supervisor","password":"wrong"}'
```

## 2. Calls

### List all calls

```bash
curl -s http://localhost:8086/api/calls \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Grab a `callId` from the response for the commands below:

```bash
export CALL_ID="paste-a-callId-here"
```

### No token at all (→ 401)

```bash
curl -i http://localhost:8086/api/calls
```

### Call detail (includes transcript/summary/evaluation content)

```bash
curl -s http://localhost:8086/api/calls/$CALL_ID \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Unknown call (→ 404)

```bash
curl -i http://localhost:8086/api/calls/does-not-exist \
  -H "Authorization: Bearer $TOKEN"
```

### Audit trail for a call

```bash
curl -s http://localhost:8086/api/calls/$CALL_ID/audit \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

## 3. Artifact actions

### Regenerate the evaluation (rule-based, no Anthropic API cost — safe to repeat)

```bash
curl -i -X POST http://localhost:8086/api/calls/$CALL_ID/artifacts/evaluation/regenerate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"model":null}'
```

Wait a couple seconds, then re-check call detail — `currentVersion` for `evaluation` should have
incremented.

### Regenerate the transcript (calls Anthropic — real API cost)

```bash
curl -i -X POST http://localhost:8086/api/calls/$CALL_ID/artifacts/transcript/regenerate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"model":"claude-opus-5"}'
```

### Unknown artifact type (→ 400)

```bash
curl -i -X POST http://localhost:8086/api/calls/$CALL_ID/artifacts/video/regenerate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"model":null}'
```

### Delete an artifact (hard delete — removes the file from disk)

```bash
curl -i -X DELETE http://localhost:8086/api/calls/$CALL_ID/artifacts/evaluation \
  -H "Authorization: Bearer $TOKEN"
```

Response includes `deletedVersion`. Re-check call detail: that artifact type should now show
`"deleted": true` with the file removed from `media-store/`, but its version history preserved.
