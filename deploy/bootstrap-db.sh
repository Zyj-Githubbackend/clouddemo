#!/usr/bin/env bash
set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-shared-mysql-1}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-123888}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_PATH="${SCRIPT_DIR}/bootstrap-db.sql"
CONTAINER_SQL_PATH="/tmp/cloud-demo-bootstrap-db.sql"

if [[ ! -f "${SQL_PATH}" ]]; then
  echo "Missing SQL bootstrap file: ${SQL_PATH}" >&2
  exit 1
fi

for i in $(seq 1 40); do
  if docker exec -e "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "${MYSQL_CONTAINER}" mysqladmin ping -h 127.0.0.1 -uroot --silent >/dev/null 2>&1; then
    break
  fi
  if [[ "${i}" -eq 40 ]]; then
    echo "MySQL container '${MYSQL_CONTAINER}' is not ready." >&2
    exit 1
  fi
  sleep 3
done

docker cp "${SQL_PATH}" "${MYSQL_CONTAINER}:${CONTAINER_SQL_PATH}"
docker exec -e "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "${MYSQL_CONTAINER}" sh -lc "mysql -uroot --default-character-set=utf8mb4 < ${CONTAINER_SQL_PATH}"
docker exec "${MYSQL_CONTAINER}" rm -f "${CONTAINER_SQL_PATH}" >/dev/null 2>&1 || true
echo "Database bootstrap completed."
