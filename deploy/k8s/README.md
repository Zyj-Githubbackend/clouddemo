# cloud-demo Kubernetes deployment

This directory contains the Kubernetes deployment for cloud-demo:

- stateless services as `Deployment replicas: 3`
- stateful middleware as `StatefulSet + PVC`
- edge entry through `edge-nginx + Ingress`
- observability stack on Kubernetes (`Prometheus + Loki + Tempo + Promtail + Grafana + OTel Collector`)

## Directory layout

- `namespaces.yaml`: `cloud-demo` and `observability` namespaces
- `cloud-demo/configmap.yaml`: shared runtime config
- `cloud-demo/secret.example.yaml`: secret template, copy to `secret.yaml` before apply
- `cloud-demo/middleware.yaml`: MySQL / Redis / RabbitMQ / MinIO / Nacos
- `cloud-demo/apps.yaml`: gateway / user / activity / announcement / feedback / monitor / mcp
- `cloud-demo/edge-nginx.yaml`: edge nginx configmap + deployment + service
- `cloud-demo/ingress.yaml`: business ingress
- `observability/configmaps.yaml`: Prometheus/Promtail/Otel/Loki/Tempo/Grafana config
- `observability/stack.yaml`: observability workloads + RBAC + ingress
- `scripts/*`: apply/delete and DB init scripts

## Images

Build and push these images before deployment:

- `cloud-demo/edge-nginx:latest`
- `cloud-demo/gateway-service:latest`
- `cloud-demo/user-service:latest`
- `cloud-demo/activity-service:latest`
- `cloud-demo/announcement-service:latest`
- `cloud-demo/feedback-service:latest`
- `cloud-demo/monitor-service:latest`
- `cloud-demo/mcp-service:latest`

If your registry path is different, update the `image` fields in:

- `deploy/k8s/cloud-demo/apps.yaml`
- `deploy/k8s/cloud-demo/edge-nginx.yaml`

## Deploy

1. Create `deploy/k8s/cloud-demo/secret.yaml`:
   - copy `deploy/k8s/cloud-demo/secret.example.yaml`
   - replace all passwords/keys
2. Apply resources:
   - Linux/macOS: `bash deploy/k8s/scripts/apply-all.sh`
   - PowerShell: `powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/apply-all.ps1`
   - The script installs ingress-nginx `controller-v1.15.1` before applying project Ingress resources.
3. Import schema and seed data:
   - Linux/macOS: `bash deploy/k8s/scripts/init-db.sh`
   - PowerShell: `powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/init-db.ps1`
   - The SQL file is shared at `deploy/common/bootstrap-db.sql`.

## Ingress Controller and edge nginx

ingress-nginx is the Kubernetes Ingress Controller. It watches `Ingress` resources and accepts traffic for hosts such as `cloud-demo.local`, then forwards that traffic to the `edge-nginx` Service.

`edge-nginx` remains the project edge workload. It serves the built frontend and applies project-specific reverse proxy rules to the gateway and backend services. ingress-nginx does not replace `edge-nginx` in this topology.

## Verify

- `kubectl -n cloud-demo get pods`
- `kubectl -n observability get pods`
- `kubectl -n cloud-demo get ingress`
- `kubectl -n observability get ingress`

Default ingress hosts:

- `cloud-demo.local`
- `grafana.cloud-demo.local`
- `prometheus.cloud-demo.local`

If needed, map these hosts in your local DNS or `/etc/hosts`.

## Local access on Docker Desktop / WSL

On Docker Desktop or other local clusters, the Ingress Controller `LoadBalancer` address may not be directly reachable from the host OS. If `kubectl -n cloud-demo get ingress` shows an address but the browser times out, use a local port-forward to the Ingress Controller:

```powershell
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 18081:80
```

Add these entries to `C:\Windows\System32\drivers\etc\hosts`:

```text
127.0.0.1 cloud-demo.local grafana.cloud-demo.local prometheus.cloud-demo.local
```

Then open:

- `http://cloud-demo.local:18081/`
- `http://grafana.cloud-demo.local:18081/`
- `http://prometheus.cloud-demo.local:18081/`

The local traffic path is:

```text
Browser -> 127.0.0.1:18081 -> ingress-nginx -> edge-nginx -> frontend / gateway / backend services
```

## Rollback / cleanup

- Linux/macOS: `bash deploy/k8s/scripts/delete-all.sh`
- PowerShell: `powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/delete-all.ps1`
