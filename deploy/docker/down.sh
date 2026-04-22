#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_ARGS=()
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  ENV_ARGS=(--env-file "${SCRIPT_DIR}/.env")
fi

docker compose -p cloud-demo "${ENV_ARGS[@]}" -f "${SCRIPT_DIR}/docker-compose.yml" down
