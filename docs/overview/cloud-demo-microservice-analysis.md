# cloud-demo 微服务架构分析

本文依据当前源码和运行配置整理，文档文件不作为架构事实来源。

## 1. 实际服务

代码中实际存在以下可启动服务：

- `gateway-service`
- `user-service`
- `activity-service`
- `announcement-service`
- `feedback-service`
- `monitor-service`
- `mcp-service`

证据：

- `services/*/src/main/java/**Application.java`
- `services/*/pom.xml`
- `services/*/src/main/resources/application.properties`

## 2. 服务职责

| 服务 | 职责 |
| --- | --- |
| `gateway-service` | API 路由、鉴权、限流、熔断 |
| `user-service` | 用户、登录、资料、角色、志愿时长 |
| `activity-service` | 活动、报名、签到、核销、图片、AI 文案 |
| `announcement-service` | 公告、附件、首页展示 |
| `feedback-service` | 反馈工单、消息、附件、管理员处理 |
| `monitor-service` | Spring Boot Admin |
| `mcp-service` | MCP 工具服务与 OAuth 端点 |

## 3. 网关与路由

`gateway-service` 在 `application.properties` 中通过以下服务名路由：

- `lb://user-service`
- `lb://activity-service`
- `lb://announcement-service`
- `lb://feedback-service`

这说明业务入口通过网关统一转发，下游服务通过注册中心服务名寻址。

## 4. 注册发现

各服务配置了 Nacos：

- `spring.cloud.nacos.server-addr`
- `spring.cloud.nacos.discovery.server-addr`

Docker 单栈和 Kubernetes 中都使用 `nacos:8848`，本机默认是 `127.0.0.1:8848`。

## 5. 服务通信

同步通信：

- 网关通过 Spring Cloud Gateway 路由到业务服务。
- `activity-service` 存在对 `user-service` 的同步调用配置。

异步通信：

- RabbitMQ 用于领域事件。
- `common-messaging` 提供消息、outbox 和消费幂等基础能力。

## 6. 数据访问

MySQL 用于业务持久化，Redis 用于缓存、限流和授权码等场景，MinIO 用于图片和附件对象存储。

证据：

- 各业务服务 `application.properties`
- `deploy/docker/docker-compose.yml`
- `deploy/k8s/cloud-demo/middleware.yaml`

## 7. 安全

登录和访问控制基于 JWT：

- `services/common` 提供 JWT 工具。
- `gateway-service` 负责鉴权与用户上下文透传。
- `mcp-service` 通过业务 JWT 判定 MCP 工具权限。

## 8. 稳定性与可观测

稳定性：

- 网关路由配置了限流和熔断。
- AI 调用、跨服务调用和 MCP 网关调用接入了 Resilience4j。

可观测：

- Actuator 暴露健康检查和 Prometheus 指标。
- OTel Collector、Tempo、Prometheus、Grafana、Loki、Promtail 组成观测链路。

证据：

- `services/*/src/main/resources/application.properties`
- `deploy/docker/observability/`
- `deploy/k8s/observability/`

## 9. 部署拓扑

当前只保留三种部署方式：

- 本机部署：`deploy/local`
- Docker 单栈部署：`deploy/docker`
- Kubernetes 部署：`deploy/k8s`

Kubernetes 部署中，业务服务通过 Deployment 多副本运行，通过 Service 负载均衡到 Ready Pod。当前没有配置 HPA 自动扩缩容。
