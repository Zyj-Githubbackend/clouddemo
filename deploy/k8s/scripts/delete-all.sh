#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SECRET_FILE="${ROOT_DIR}/deploy/k8s/cloud-demo/secret.yaml"
INGRESS_NGINX_MANIFEST="https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/cloud/deploy.yaml"

kubectl delete -f "${ROOT_DIR}/deploy/k8s/observability/stack.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/observability/configmaps.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/cloud-demo/ingress.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/cloud-demo/edge-nginx.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/cloud-demo/apps.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/cloud-demo/middleware.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/deploy/k8s/cloud-demo/configmap.yaml" --ignore-not-found

if [[ -f "${SECRET_FILE}" ]]; then
  kubectl delete -f "${SECRET_FILE}" --ignore-not-found
fi

kubectl delete -f "${ROOT_DIR}/deploy/k8s/namespaces.yaml" --ignore-not-found
kubectl delete -f "${INGRESS_NGINX_MANIFEST}" --ignore-not-found
echo "Delete requests submitted."
