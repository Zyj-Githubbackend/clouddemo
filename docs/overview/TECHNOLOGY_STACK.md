# 技术说明

本文基于当前源码和运行配置整理。文档只描述当前保留的本机部署、Docker 单栈部署和 Kubernetes 部署。

## 1. 后端

证据：

- `pom.xml`
- `services/pom.xml`
- `services/*/pom.xml`
- `services/*/src/main/resources/application.properties`

技术：

- Java 17
- Maven 多模块工程
- Spring Boot 3
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos
- OpenFeign / Spring Cloud LoadBalancer
- MyBatis-Plus
- Spring Boot Actuator
- Micrometer Tracing
- Resilience4j

服务：

- `gateway-service`
- `user-service`
- `activity-service`
- `announcement-service`
- `feedback-service`
- `monitor-service`
- `mcp-service`

## 2. 前端

证据：

- `frontend2/package.json`
- `frontend2/src/`

技术：

- Vue 3
- Vite
- Element Plus
- Pinia
- Axios

## 3. 中间件

证据：

- `deploy/docker/docker-compose.yml`
- `deploy/k8s/cloud-demo/middleware.yaml`
- 各服务 `application.properties`

组件：

- MySQL：业务数据持久化
- Redis：限流、防超卖、授权码等缓存能力
- RabbitMQ：异步事件和服务解耦
- Nacos：服务注册发现
- MinIO：图片和附件对象存储

## 4. 网关与入口

证据：

- `services/gateway-service/src/main/resources/application.properties`
- `deploy/local/nginx/cloud-demo.local.conf`
- `deploy/docker/edge-nginx/nginx.conf`
- `deploy/k8s/cloud-demo/ingress.yaml`
- `deploy/k8s/cloud-demo/edge-nginx.yaml`

入口关系：

- 本机：本机 Nginx 代理到 `gateway-service`
- Docker：`edge-nginx` 代理到 `gateway-service`
- K8s：`ingress-nginx` 代理到 `edge-nginx` Service，再进入业务网关

## 5. 可观测

证据：

- `deploy/docker/observability/`
- `deploy/k8s/observability/`
- 各服务 `management.*` 配置

组件：

- Prometheus：指标
- Grafana：展示
- Loki：日志
- Promtail：日志采集
- Tempo：Trace 存储
- OTel Collector：Trace 接收与转发

## 6. 部署

当前只保留三种部署方式：

| 方式 | 配置位置 | 说明 |
| --- | --- | --- |
| 本机部署 | `deploy/local` | 本机运行服务，Nginx 统一入口 |
| Docker 单栈 | `docker-compose.yml` / `deploy/docker` | 每个服务一个容器 |
| Kubernetes | `deploy/k8s` | Deployment / StatefulSet / Service / Ingress |

## 7. 测试

证据：

- `services/*/src/test/`
- `pom.xml`

项目包含 JUnit、Mockito、Testcontainers 等测试依赖，用于服务层和部分集成场景验证。
