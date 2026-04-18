#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "${SCRIPT_DIR}/up-shared.sh"
bash "${SCRIPT_DIR}/up-stack-a.sh"
bash "${SCRIPT_DIR}/up-stack-b.sh"
bash "${SCRIPT_DIR}/up-edge.sh"
bash "${SCRIPT_DIR}/verify-health.sh"
