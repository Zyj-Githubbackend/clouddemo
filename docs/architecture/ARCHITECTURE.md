# 系统架构说明

本文基于当前源码和部署配置整理，部署相关事实以以下文件为证据：

- 后端模块：`services/*/pom.xml`
- 服务配置：`services/*/src/main/resources/application.properties`
- 本机部署：`deploy/local/nginx/cloud-demo.local.conf`
- Docker 部署：`deploy/docker/docker-compose.yml`
- Kubernetes 部署：`deploy/k8s/cloud-demo/*.yaml`、`deploy/k8s/observability/*.yaml`

## 1. 服务划分

当前代码中实际存在的微服务模块：

| 服务 | 端口 | 职责 |
| --- | --- | --- |
| `gateway-service` | `9000` | 统一 API 入口、路由、JWT 鉴权、限流与熔断 |
| `user-service` | `8100` | 用户、登录、资料、角色、志愿时长 |
| `activity-service` | `8200` | 活动、报名、签到、核销、AI 文案、活动图片 |
| `announcement-service` | `8300` | 公告、附件、首页公告 |
| `feedback-service` | `8400` | 意见反馈工单、回复、附件、管理员处理 |
| `monitor-service` | `9100` | Spring Boot Admin 监控中心 |
| `mcp-service` | `9300` | MCP Server 与 OAuth 相关端点 |

## 2. 调用关系

```text
Browser
  -> Nginx / edge-nginx / Ingress
  -> gateway-service
  -> user-service / activity-service / announcement-service / feedback-service
```

`gateway-service` 的路由来自 `services/gateway-service/src/main/resources/application.properties`，通过 `lb://user-service`、`lb://activity-service`、`lb://announcement-service`、`lb://feedback-service` 访问后端服务。

`activity-service` 对 `user-service` 仍存在同步调用配置，服务名来自 `USER_SERVICE_NAME` / `user-service`。

## 3. 服务注册与发现

所有主要后端服务通过 Nacos 注册发现：

- 配置证据：`spring.cloud.nacos.server-addr`
- 默认本机地址：`127.0.0.1:8848`
- Docker 单栈地址：`nacos:8848`
- Kubernetes 地址：`nacos:8848`

## 4. 数据与中间件

| 组件 | 用途 |
| --- | --- |
| MySQL | 各业务库持久化 |
| Redis | 网关限流、活动防超卖、MCP 授权码等 |
| RabbitMQ | 服务间异步事件 |
| MinIO | 活动、公告、反馈附件与图片 |
| Nacos | 服务注册发现 |

本机、Docker 和 Kubernetes 共用初始化 SQL：

- `deploy/common/bootstrap-db.sql`

## 5. 部署拓扑

### 本机部署

```text
Browser -> local Nginx -> gateway-service / monitor-service / mcp-service
```

配置：

- `deploy/local/nginx/cloud-demo.local.conf`

### Docker 单栈

```text
Browser -> edge-nginx -> gateway-service -> business services
```

配置：

- `deploy/docker/docker-compose.yml`
- `deploy/docker/edge-nginx/nginx.conf`

每个服务只保留一个容器实例。

### Kubernetes

```text
Browser
  -> ingress-nginx
  -> edge-nginx Service
  -> edge-nginx Pod
  -> gateway-service Service
  -> gateway-service Pod
```

配置：

- `deploy/k8s/cloud-demo/ingress.yaml`
- `deploy/k8s/cloud-demo/edge-nginx.yaml`
- `deploy/k8s/cloud-demo/apps.yaml`
- `deploy/k8s/cloud-demo/middleware.yaml`

业务服务在 K8s 中是 3 副本 Deployment，Kubernetes Service 负责在 Ready Pod 之间分发流量。

## 6. 可观测

可观测组件：

- Prometheus
- Grafana
- Loki
- Promtail
- Tempo
- OpenTelemetry Collector

Docker 配置位于：

- `deploy/docker/observability/`

Kubernetes 配置位于：

- `deploy/k8s/observability/`

## 7. 架构评价

本项目具备典型微服务特征：

- 多个独立 Spring Boot 服务
- 服务注册发现
- 网关统一入口
- 服务间同步调用和 RabbitMQ 异步事件
- 分库持久化配置
- 独立观测组件
- Docker 与 Kubernetes 部署清单

当前需要明确的限制：

- Docker 部署是单栈单实例，不提供多副本负载均衡。
- Kubernetes 部署提供固定副本负载均衡，但当前没有配置 HPA 自动扩容。
- MySQL、Redis、RabbitMQ、Nacos、MinIO 在当前 K8s 清单中都是单副本 StatefulSet。
