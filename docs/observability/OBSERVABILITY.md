# 可观测栈说明

本项目保留两套可观测部署方式：

- Docker 单栈：`deploy/docker/observability`
- Kubernetes：`deploy/k8s/observability`

## 1. 组件

- `otel-collector`：接收应用 trace
- `prometheus`：指标抓取与时序数据存储
- `grafana`：统一观测入口
- `loki`：日志存储
- `promtail`：日志采集
- `tempo`：链路追踪存储

## 2. Docker 单栈

配置文件：

- `deploy/docker/observability/prometheus.yml`
- `deploy/docker/observability/promtail-config.yml`
- `deploy/docker/observability/loki-config.yml`
- `deploy/docker/observability/tempo.yml`
- `deploy/docker/observability/otel-collector-config.yml`
- `deploy/docker/observability/grafana/provisioning/datasources/datasources.yml`

访问：

- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

Grafana 默认账号：

- 用户名：`admin`
- 密码：`admin`

Docker 单栈中每个应用服务只有一个容器，因此指标和日志按服务名查看即可，例如：

```logql
{service="gateway-service"}
```

## 3. Kubernetes

Kubernetes 观测配置位于：

- `deploy/k8s/observability/configmaps.yaml`
- `deploy/k8s/observability/stack.yaml`

本机 Docker Desktop 可通过 Ingress Controller 端口转发访问：

```powershell
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 18081:80
```

访问：

- `http://grafana.cloud-demo.local:18081/`
- `http://prometheus.cloud-demo.local:18081/`

## 4. 数据流

指标：

```text
Spring Boot Actuator -> Prometheus -> Grafana
```

日志：

```text
Container stdout / log files -> Promtail -> Loki -> Grafana
```

链路追踪：

```text
Micrometer Tracing -> OTel Collector -> Tempo -> Grafana
```

## 5. 注意事项

- Docker 单栈的 Promtail 会挂载 Docker socket，用于采集容器日志。
- Kubernetes 的 Promtail 以 DaemonSet 形式运行。
- 本项目当前没有启用 HPA，Kubernetes 只负责在固定副本之间做 Service 负载均衡。
