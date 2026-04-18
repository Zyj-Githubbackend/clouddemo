#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"
docker compose -p edge -f compose.edge.yml down
docker compose -p stack-b --env-file deploy/stack-b.env -f compose.stack.yml down
docker compose -p stack-a --env-file deploy/stack-a.env -f compose.stack.yml down
docker compose -p shared -f compose.shared.yml down
