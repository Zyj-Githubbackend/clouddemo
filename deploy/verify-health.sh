#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

wait_container() {
  local name="$1"
  local timeout_seconds="${2:-600}"
  local waited=0

  while (( waited < timeout_seconds )); do
    local status=""
    if status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${name}" 2>/dev/null)"; then
      if [[ "${status}" == "healthy" || "${status}" == "running" ]]; then
        return 0
      fi
      if [[ "${status}" == "unhealthy" || "${status}" == "exited" || "${status}" == "dead" ]]; then
        echo "Container '${name}' is in bad status: ${status}" >&2
        return 1
      fi
    fi
    sleep 3
    waited=$(( waited + 3 ))
  done

  echo "Timeout waiting for container '${name}' to become healthy/running." >&2
  return 1
}

wait_container "shared-mysql-1" 600
wait_container "shared-mcp-service-1" 600
wait_container "stack-a-gateway-service-1" 600
wait_container "stack-b-gateway-service-1" 600
wait_container "edge-edge-nginx-1" 600

payload='{"username":"admin","password":"password123"}'

resp_a="$(curl -sS -H 'Content-Type: application/json' -H 'X-Stack-Id: a' -d "${payload}" 'http://localhost:8081/api/user/login')"
if ! grep -Eq '"code"[[:space:]]*:[[:space:]]*200' <<<"${resp_a}"; then
  echo "Stack A login smoke test failed: ${resp_a}" >&2
  exit 1
fi

resp_b="$(curl -sS -H 'Content-Type: application/json' -H 'X-Stack-Id: b' -d "${payload}" 'http://localhost:8081/api/user/login')"
if ! grep -Eq '"code"[[:space:]]*:[[:space:]]*200' <<<"${resp_b}"; then
  echo "Stack B login smoke test failed: ${resp_b}" >&2
  exit 1
fi

echo "Health verification passed."
