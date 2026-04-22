#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SQL_FILE="${ROOT_DIR}/deploy/common/bootstrap-db.sql"

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "Missing ${SQL_FILE}"
  exit 1
fi

kubectl -n cloud-demo rollout status statefulset/mysql --timeout=300s
MYSQL_POD="$(kubectl -n cloud-demo get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"

if [[ -z "${MYSQL_POD}" ]]; then
  echo "MySQL pod was not found in namespace cloud-demo."
  exit 1
fi

kubectl -n cloud-demo exec -i "${MYSQL_POD}" -- sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"' < "${SQL_FILE}"
echo "Imported ${SQL_FILE} into MySQL pod ${MYSQL_POD}."
