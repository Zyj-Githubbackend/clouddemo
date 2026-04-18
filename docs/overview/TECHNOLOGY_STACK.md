# 项目技术说明

## 1. 文档目的

本文档基于**仓库源码**与**当前运行中的 Docker 容器**整理，目标是说明：

1. 这个项目实际用了哪些技术；
2. 每种技术在本项目里承担什么职责、解决什么问题；
3. 这些技术分别落在哪些模块、配置或容器里。

本文档不是泛泛的“技术栈列表”，而是面向本项目的**技术作用说明书**。

阅读方式：每种技术都尽量从三个角度说明：

1. **证据文件**：证明项目确实使用了它；
2. **技术作用**：它在项目里负责解决什么问题；
3. **落地方式**：它具体落在哪些服务、容器或配置里。

---

## 2. 运行态容器核对结果

在当前环境中，已经确认这些技术组件正在 Docker 中运行：

- 业务微服务：`gateway-service`、`user-service`、`activity-service`、`announcement-service`、`feedback-service`、`monitor-service`、`mcp-service`
- 基础设施：`MySQL`、`Redis`、`MinIO`、`Nacos`、`RabbitMQ`
- 统一入口：`edge-nginx`
- 可观测栈：`Prometheus`、`Grafana`、`Loki`、`Promtail`、`Tempo`、`OpenTelemetry Collector`

实际核对到的容器内容包括：

- `stack-a-gateway-service-1`：存在 `/app/app.jar` 与 `/app/logs/debug.log`
- `shared-mysql-1`：存在 `/docker-entrypoint-initdb.d/01-init.sql`
- `shared-minio-1`：对象目录中已有 `activity-images`
- `shared-prometheus-1`：存在 `/etc/prometheus/prometheus.yml`
- `shared-grafana-1`：存在 `/etc/grafana/provisioning`

这说明本文档提到的关键技术并不是“计划中使用”，而是已经在当前项目里真实落地。

---

## 3. 总体技术分层

本项目可以分成 6 层技术：

1. **后端基础框架层**：Java、Maven、Spring Boot、Spring Cloud
2. **业务服务层**：Gateway、用户、活动、公告、反馈、MCP、监控
3. **数据与中间件层**：MySQL、Redis、RabbitMQ、Nacos、MinIO
4. **前端层**：Vue 3、Vite、Pinia、Element Plus 等
5. **可观测与稳定性层**：Actuator、Micrometer、OTel、Prometheus、Grafana、Loki、Tempo、Resilience4j
6. **部署与测试层**：Docker Compose、Nginx、JUnit、Testcontainers

---

## 4. 后端基础框架与语言

### 4.1 Java 17

**证据文件**

- `pom.xml`
- `services/*/pom.xml`
- 各服务 Dockerfile 的运行时镜像参数

**在本项目中的作用**

- 所有后端微服务都运行在 Java 17 上。
- 用于承载 Spring Boot 3.x、Spring Cloud 2023、Spring AI MCP 等现代依赖。
- Docker 镜像中统一采用 Java 17 运行时镜像启动 `app.jar`。

### 4.2 Maven

**证据文件**

- `pom.xml`
- `services/pom.xml`

**在本项目中的作用**

- Maven 负责整个后端的多模块构建。
- 根工程 `cloud-demo` 只聚合 `services` 模块。
- `services/pom.xml` 再进一步聚合 `common`、`common-messaging`、`gateway-service`、`user-service`、`activity-service`、`announcement-service`、`feedback-service`、`monitor-service`、`mcp-service`。
- Docker 构建时也通过 Maven 进行后端打包。

### 4.3 Spring Boot

**证据文件**

- `pom.xml`（大多数服务继承 Boot 3.3.4 体系）
- `services/mcp-service/pom.xml`（MCP 独立使用 Boot 3.4.2）

**在本项目中的作用**

- 所有后端服务都建立在 Spring Boot 上。
- 提供 Web API、Actuator、配置绑定、依赖注入、测试支持等基础能力。
- `mcp-service` 单独采用较新的 Boot 版本，主要是为了匹配 Spring AI MCP 相关依赖。

### 4.4 Spring Cloud

**证据文件**

- `pom.xml` 中的 `spring-cloud.version=2023.0.3`
- `services/gateway-service/pom.xml`
- `docs/architecture/ARCHITECTURE.md`

**在本项目中的作用**

- 用于微服务网关、服务发现、负载均衡等微服务能力。
- 主要落地在：
  - `gateway-service`：统一入口与路由
  - 各业务服务：通过 Nacos 做服务注册与发现
  - `mcp-service`：通过网关访问业务 API

### 4.5 Spring Cloud Gateway

**证据文件**

- `services/gateway-service/pom.xml`
- `docs/architecture/ARCHITECTURE.md`

**在本项目中的作用**

- `gateway-service` 不是传统 MVC 网关，而是基于 Spring Cloud Gateway 实现。
- 负责：
  - 对外统一入口
  - 路由转发到 `user/activity/announcement/feedback`
  - JWT 校验
  - 补充和透传 `X-User-Id`、`X-Username`、`X-User-Role`、`X-Trace-Id`

### 4.6 Spring Cloud Alibaba + Nacos

**证据文件**

- `pom.xml` 中 `spring-cloud-alibaba.version=2023.0.3.2`
- 多个服务 `pom.xml` 中的 `spring-cloud-starter-alibaba-nacos-discovery`
- `compose.stack.yml`

**在本项目中的作用**

- 每个 A/B 栈里都运行一个 `nacos` 实例。
- 业务服务通过 Nacos 完成服务注册与发现。
- 这样网关、监控、服务间调用都能基于服务名工作，而不是写死容器 IP。

---

## 5. 后端业务与应用层技术

### 5.1 Spring MVC / Web

**证据文件**

- `services/user-service/pom.xml`
- `services/activity-service/pom.xml`
- `services/announcement-service/pom.xml`
- `services/feedback-service/pom.xml`
- `services/monitor-service/pom.xml`
- `services/mcp-service/pom.xml`

**在本项目中的作用**

- 除 `gateway-service` 外，大多数服务都基于 `spring-boot-starter-web` 暴露 HTTP API。
- 用于实现：
  - 用户登录注册与资料维护
  - 活动管理、报名、签到、核销
  - 公告管理与附件下载
  - 意见反馈工单与附件处理
  - MCP OAuth 与工具接口适配

### 5.2 Spring AI MCP Server

**证据文件**

- `services/mcp-service/pom.xml`
- `docs/mcp/MCP_CONNECTION.md`

**在本项目中的作用**

- `mcp-service` 使用 `spring-ai-starter-mcp-server-webmvc` 对外提供 MCP 服务。
- 暴露 `/mcp`，支持 Streamable HTTP MCP 协议。
- 把用户、活动、公告、反馈等业务能力封装成 MCP tools，供客户端调用。
- 同时结合 OAuth 授权码流程，实现受保护的 MCP 访问。

### 5.3 Spring Boot Admin

**证据文件**

- `services/monitor-service/pom.xml`
- `services/mcp-service/pom.xml`（Admin Client）
- `docs/architecture/ARCHITECTURE.md`

**在本项目中的作用**

- `monitor-service` 是整个系统的 Spring Boot Admin Server。
- 用来查看各服务的健康状态、Actuator 信息和运行概况。
- `mcp-service` 已接入 Admin Client；其它服务则通过观测栈和 Actuator 暴露指标。

### 5.4 MyBatis-Plus

**证据文件**

- `services/activity-service/pom.xml`
- `services/user-service/pom.xml`
- `services/announcement-service/pom.xml`
- `services/feedback-service/pom.xml`

**在本项目中的作用**

- 作为主要 ORM / 数据访问框架。
- 用来操作：
  - 用户表 `sys_user`
  - 活动表 `vol_activity`
  - 报名表 `vol_registration`
  - 公告表与公告附件表
  - 反馈工单与附件表
  - MQ 幂等消费记录与 outbox 记录

### 5.5 JWT（JJWT）

**证据文件**

- `services/gateway-service/pom.xml`
- `services/user-service/pom.xml`
- `services/mcp-service/pom.xml`

**在本项目中的作用**

- 用户登录后签发 JWT。
- `gateway-service` 负责校验业务 JWT，并把用户身份信息透传给下游服务。
- `mcp-service` 在 OAuth 登录后也会复用业务 JWT 去调用网关。

### 5.6 BCrypt / spring-security-crypto

**证据文件**

- `services/user-service/pom.xml`
- `database/init.sql`（密码以 BCrypt 哈希形式入库）

**在本项目中的作用**

- 这里只用到了 Spring Security 的密码加密能力，并没有引入整套 Spring Security Web。
- 用户密码使用 BCrypt 进行哈希存储与校验。

### 5.7 Resilience4j

**证据文件**

- `services/activity-service/pom.xml`
- `services/mcp-service/pom.xml`
- 各自 `application.properties`

**在本项目中的作用**

- 给剩余同步高风险链路增加弹性保护。
- 已经明确落在：
  - `mcp-service -> gateway` 调用链
  - `activity-service -> 外部 AI API` 调用链
- 提供 `retry`、`circuit breaker`、`bulkhead` 相关保护能力。

### 5.8 Apache POI

**证据文件**

- `services/activity-service/pom.xml`
- `docs/architecture/ARCHITECTURE.md`

**在本项目中的作用**

- 用于导出“我的志愿足迹”Excel。
- 也说明项目不只是做 CRUD，还支持管理/导出类办公场景。

### 5.9 MinIO Java SDK

**证据文件**

- `services/activity-service/pom.xml`
- `services/announcement-service/pom.xml`
- `services/feedback-service/pom.xml`

**在本项目中的作用**

- 后端通过 MinIO SDK 上传和读取对象。
- 存储内容包括：
  - 活动图片
  - 公告图片
  - 公告附件
  - 反馈附件

### 5.10 Lombok

**证据文件**

- 多个服务 `pom.xml`

**在本项目中的作用**

- 用于减少 Java 样板代码，如 getter/setter、构造器、日志字段等。
- 属于开发效率技术，不是业务核心，但在项目里是实际使用的。

---

## 6. 数据、注册中心与消息中间件

### 6.1 MySQL

**证据文件**

- `compose.shared.yml`
- `database/init.sql`
- `services/*/pom.xml` 中的 `mysql-connector-j`

**在本项目中的作用**

- MySQL 是主要业务数据库。
- 保存：
  - 用户数据
  - 活动与报名数据
  - 公告与附件元数据
  - 反馈工单与附件元数据
  - event outbox
  - MQ 幂等消费记录

### 6.2 Redis

**证据文件**

- `compose.shared.yml`
- `services/activity-service/pom.xml`
- `docs/architecture/ARCHITECTURE.md`

**在本项目中的作用**

- `activity-service` 使用 Redis 做报名库存控制。
- 报名前先扣 Redis，再写 MySQL，失败再回滚库存，用于降低超卖风险。

### 6.3 RabbitMQ

**证据文件**

- `compose.stack.yml`
- `services/common-messaging/pom.xml`
- `database/init.sql` 中 `event_outbox` 与 `mq_consume_record`

**在本项目中的作用**

- RabbitMQ 是项目里的异步消息总线。
- 当前主要承担：
  - `activity-service -> announcement-service`
  - `feedback-service -> user-service`
  - `activity-service -> user-service` 的 `user.updated` 异步链路
- 配合 outbox + 幂等消费记录，实现最终一致性而不是脆弱的同步链路。

### 6.4 common-messaging（项目内消息基础设施）

**证据文件**

- `services/common-messaging/pom.xml`
- `database/init.sql`

**在本项目中的作用**

- 这是项目自建的消息基础设施模块，不是第三方中间件本身。
- 主要负责：
  - 事件模型
  - Rabbit 配置常量
  - outbox 发布支持
  - 消费幂等记录
- 它让多个业务服务可以复用统一的消息模式，而不是各自手写。

### 6.5 Nacos

**证据文件**

- `compose.stack.yml`
- 多个服务 `pom.xml`

**在本项目中的作用**

- 作为服务注册与发现中心。
- A/B 两个栈各自有独立的 Nacos，避免两套环境互相污染。

### 6.6 MinIO

**证据文件**

- `compose.shared.yml`
- `services/*-service/pom.xml`（与对象存储相关的模块）

**在本项目中的作用**

- 统一对象存储。
- 容器中已经确认存在 `activity-images` 桶/目录。
- 支撑活动图片、公告图片、公告附件、反馈附件的上传与下载。

---

## 7. 可观测性与稳定性技术

### 7.1 Spring Boot Actuator

**证据文件**

- 多个服务 `pom.xml`
- 各服务 `application.properties`

**在本项目中的作用**

- 所有服务都通过 Actuator 暴露健康检查与指标端点。
- Docker 健康检查、Prometheus 抓取、Spring Boot Admin 都依赖 Actuator。

### 7.2 Micrometer

**证据文件**

- 各服务 `pom.xml` 中的 `micrometer-registry-prometheus`
- 各服务 `application.properties`

**在本项目中的作用**

- Micrometer 是应用指标抽象层。
- 指标最终导出给 Prometheus。

### 7.3 OpenTelemetry（OTel）

**证据文件**

- 各服务 `pom.xml` 中的 `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`
- `compose.shared.yml` 中 `otel-collector`

**在本项目中的作用**

- 应用 trace 通过 OTLP 发往 OTel Collector。
- Collector 再把 trace 送到 Tempo。
- 这样 Grafana 里可以看到链路追踪，而不是只有日志和指标。

### 7.4 Prometheus

**证据文件**

- `compose.shared.yml`
- `deploy/observability/prometheus.yml`

**在本项目中的作用**

- Prometheus 负责抓取所有服务的 `/actuator/prometheus`。
- 已经改成**按容器发现**，可以看到 A/B 栈每个副本的独立 target。
- 例如：`stack-a-user-service-2`、`stack-a-user-service-3`。

### 7.5 Grafana

**证据文件**

- `compose.shared.yml`
- `deploy/observability/grafana/provisioning/datasources/datasources.yml`

**在本项目中的作用**

- 作为观测统一入口。
- 已接入：
  - Prometheus（指标）
  - Loki（日志）
  - Tempo（链路）

### 7.6 Loki

**证据文件**

- `compose.shared.yml`
- `deploy/observability/loki-config.yml`

**在本项目中的作用**

- 作为集中式日志存储。
- 当前已支持按容器实例标签查询日志，例如：
  - `stack-a-user-service-2`
  - `shared-mcp-service-1`

### 7.7 Promtail

**证据文件**

- `compose.shared.yml`
- `deploy/observability/promtail-config.yml`

**在本项目中的作用**

- Promtail 负责把日志送进 Loki。
- 当前方案为：
  - 主路径：按 Docker 容器发现采集业务服务实例日志
  - 辅路径：保留 `edge` 文件日志兜底
- 已持久化 `positions.yaml`，避免重启后反复重扫旧日志。

### 7.8 Tempo

**证据文件**

- `compose.shared.yml`
- `deploy/observability/tempo.yml`

**在本项目中的作用**

- Tempo 用于存储和查询分布式链路追踪数据。
- 通过 OTel Collector 接收应用 trace。

### 7.9 OpenTelemetry Collector

**证据文件**

- `compose.shared.yml`
- `deploy/observability/otel-collector-config.yml`

**在本项目中的作用**

- 作为 trace 汇聚器。
- 解耦应用和具体 trace 后端（Tempo），让应用统一对 Collector 发 OTLP 即可。

---

## 8. 前端技术

### 8.1 Vue 3

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 整个前端页面与后台管理界面的核心框架。
- 用于实现登录、活动、公告、反馈、个人中心、管理员页面等所有前端视图。

### 8.2 Vite

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 前端开发服务器与构建工具。
- 开发时提供 `npm run dev`，生产时构建 `frontend2/dist` 给 Nginx 托管。

### 8.3 Vue Router

**证据文件**

- `frontend2/package.json`
- `frontend2/README.md`

**在本项目中的作用**

- 负责前端路由管理。
- 用于区分用户侧和管理员侧页面。

### 8.4 Pinia

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 作为前端状态管理工具。
- 适合保存用户登录态、页面共享状态等。

### 8.5 Axios

**证据文件**

- `frontend2/package.json`
- `frontend2/README.md`

**在本项目中的作用**

- 前端 HTTP 请求客户端。
- 统一请求实例定义在 `src/utils/request.js`，负责调用 `/api/*` 接口。

### 8.6 Element Plus

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 作为前端 UI 组件库。
- 用于表单、表格、对话框、布局和管理后台界面。

### 8.7 ECharts

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 用于前端图表展示。
- 一般适合活动统计、时长汇总等可视化页面。

### 8.8 Day.js

**证据文件**

- `frontend2/package.json`

**在本项目中的作用**

- 用于前端时间格式化与日期处理。
- 尤其适合活动时间、招募阶段、公告发布时间等展示逻辑。

---

## 9. 部署与运行技术

### 9.1 Docker Compose

**证据文件**

- `compose.shared.yml`
- `compose.stack.yml`
- `compose.edge.yml`

**在本项目中的作用**

- 项目使用 Docker Compose 组织整套运行环境。
- 当前采用：
  - `shared`：共享基础设施与观测组件
  - `stack-a`：A 栈业务服务
  - `stack-b`：B 栈业务服务
  - `edge`：统一入口

### 9.2 Nginx / edge-nginx

**证据文件**

- `deploy/nginx/cloud-demo.local.conf`
- `deploy/edge-nginx/`
- `compose.edge.yml`

**在本项目中的作用**

- 本机开发模式通过 Nginx 做统一入口。
- Docker 模式通过 `edge-nginx` 暴露 `8081`，负责：
  - 前端页面
  - `/api/**`
  - `/monitor/a/**` 与 `/monitor/b/**`
  - MCP OAuth 与 `/mcp`

### 9.3 PowerShell / Shell 启动脚本

**证据文件**

- `deploy/*.ps1`
- `deploy/*.sh`

**在本项目中的作用**

- 提供分阶段启动与一键部署脚本。
- 同时承载 A/B 栈部署参数和副本数控制。

---

## 10. 测试技术

### 10.1 JUnit + Spring Boot Test

**证据文件**

- 多个服务 `pom.xml`
- `docs/overview/PROJECT_SUMMARY.md`

**在本项目中的作用**

- 用于单元测试和模块测试。
- 已有测试覆盖 JWT 工具、用户服务、活动时间校验、活动服务逻辑等。

### 10.2 Testcontainers

**证据文件**

- `services/user-service/pom.xml`

**在本项目中的作用**

- 用于真实基础设施集成测试。
- 当前已在 `user-service` 测试链路中引入 MySQL 与 RabbitMQ 容器测试依赖，验证消息消费相关逻辑。

---

## 11. 技术之间的配合关系

本项目不是“堆技术”，而是有明确分工：

- **Spring Boot / Spring Cloud**：提供微服务应用骨架
- **Nacos**：让服务能够互相找到
- **Gateway**：负责统一入口和鉴权
- **MySQL / Redis / RabbitMQ / MinIO**：分别承担结构化数据、并发控制、异步解耦、对象存储
- **Prometheus / Grafana / Loki / Tempo / OTel**：分别承担指标、展示、日志、链路与采集转发
- **Docker Compose + Nginx**：负责把整套系统组织成可本地部署、可 A/B 演练的环境
- **Vue 3 + Vite + Element Plus**：负责前端交互与管理界面

换句话说，这个项目本质上是一个：

> **基于 Spring Boot 微服务 + Docker A/B 双栈 + RabbitMQ 最终一致性 + 完整可观测栈 + Vue 3 前端的校园志愿服务平台。**

---

## 12. 建议阅读顺序

如果你要继续理解项目，推荐按下面顺序阅读：

1. [项目交付摘要](PROJECT_SUMMARY.md)
2. [架构说明](../architecture/ARCHITECTURE.md)
3. [部署说明](../deploy/DEPLOY.md)
4. [MCP 连接说明](../mcp/MCP_CONNECTION.md)
5. 本文档（技术说明）
