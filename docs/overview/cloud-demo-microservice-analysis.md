# 《cloud-demo项目中微服务架构技术的应用分析》

本报告仅依据仓库中的 `pom.xml`、`application.properties`、`src/main/java` 与 `docker compose` 配置整理，重点说明各类微服务技术在项目中的实际落地方式，而不是只列出依赖名称。

## 一、项目整体微服务架构概述

从代码和部署配置看，`cloud-demo` 已经具备真实微服务系统的核心特征，而不只是 Maven 多模块拆分。首先，仓库中存在独立可启动的网关、用户、活动、公告、反馈、监控和 MCP 服务；其次，这些服务在 Compose 中被定义为独立容器，并且支持按服务单独扩容；再次，服务间既有通过注册中心和网关完成的同步调用，也有通过 RabbitMQ 完成的异步事件传播。

这个架构的整体形态可以概括为“三层部署”：底层是共享基础设施，包括 MySQL、Redis、MinIO、OTel Collector、Prometheus、Grafana、Loki、Tempo 和 MCP 服务；中间层是业务栈，包含 Nacos、RabbitMQ、gateway-service、user-service、activity-service、announcement-service、feedback-service、monitor-service；最外层是 edge-nginx，负责把外部请求分发到 stack-a 或 stack-b。也就是说，项目不仅在代码结构上拆了服务，在运行时拓扑上也真正区分了网关层、业务层、监控层和边缘入口层。

从实现成熟度看，它属于“轻量但完整”的微服务实践：已经把注册发现、统一入口、服务通信、异步消息、对象存储、可观测性和弹性保护接进了真实业务流程；但基础设施仍然以共享实例为主，配置中心尚未真正落地，安全与治理能力还有进一步强化空间。

证据文件：

- `pom.xml`
- `services/pom.xml`
- `compose.shared.yml`
- `compose.stack.yml`
- `compose.edge.yml`
- `deploy/edge-nginx/nginx.conf`

## 二、服务拆分与职责划分

- `gateway-service` 是统一入口，负责路由转发、JWT 校验、用户身份透传、限流和熔断降级。
- `user-service` 负责用户注册、登录、个人资料维护、密码修改和志愿时长管理，同时提供内部用户摘要接口供其他服务调用。
- `activity-service` 负责活动创建、修改、报名、取消报名、签到、工时确认、图片上传以及 AI 文案生成，是业务逻辑最集中的服务。
- `announcement-service` 负责公告管理、公告图片和附件管理，并通过消费活动事件维护本地活动投影数据。
- `feedback-service` 负责反馈工单、消息回复、状态流转和附件管理，并在创建反馈后发布领域事件。
- `monitor-service` 负责 Spring Boot Admin 监控中心，作为可观测性入口之一。
- `mcp-service` 不是核心业务服务，而是面向 AI Agent 的工具服务。它把现有平台能力封装为 MCP Tools，并通过网关访问原有业务接口。
- `common` 与 `common-messaging` 不是运行中服务，而是公共能力模块，分别沉淀通用返回结构、异常、JWT 工具、Redis Key 规则，以及 MQ 常量、RabbitMQ 配置、幂等辅助与 outbox 模型。

这种拆分说明项目的服务边界并不是按“页面”随意切分，而是围绕用户、活动、公告、反馈这几类核心业务对象建立服务职责，再由网关、监控和 MCP 服务承担平台级横切能力。

证据文件：

- `services/gateway-service/src/main/java/org/example/GatewayApplication.java`
- `services/user-service/src/main/java/org/example/UserApplication.java`
- `services/activity-service/src/main/java/org/example/ActivityApplication.java`
- `services/announcement-service/src/main/java/org/example/AnnouncementApplication.java`
- `services/feedback-service/src/main/java/org/example/FeedbackApplication.java`
- `services/monitor-service/src/main/java/org/example/MonitorApplication.java`
- `services/mcp-service/src/main/java/org/example/mcp/McpApplication.java`

## 三、微服务核心技术栈及其在项目中的应用

| 技术 | 版本 | 在项目中的具体应用 | 关键证据 |
|---|---|---|---|
| Spring Boot | 主服务 `3.3.4`，MCP 服务 `3.4.2` | 作为所有服务的运行时基础，承载 Web、WebFlux、Actuator、自动配置与容器化启动 | `pom.xml`，`services/mcp-service/pom.xml` |
| Spring Cloud | `2023.0.3` | 提供网关、服务发现整合、负载均衡与熔断集成能力 | `pom.xml` |
| Spring Cloud Alibaba / Nacos | `2023.0.3.2` | 用作服务注册与发现中心，支撑 `lb://服务名` 路由与服务名调用 | `pom.xml`，各服务 `application.properties` |
| Gateway | 版本由 Spring Cloud BOM 管理 | 统一入口、路径重写、JWT 鉴权、限流、路由级熔断与降级 | `services/gateway-service/pom.xml`，`services/gateway-service/src/main/resources/application.properties` |
| Spring MVC / WebFlux | Gateway 为 WebFlux，其余主业务服务为 MVC | 形成“响应式入口 + 同步业务服务”的边界分工 | `services/gateway-service/pom.xml`，各业务服务 `pom.xml` |
| MyBatis-Plus | `3.5.9` | 作为主要数据访问框架，承担 CRUD、条件查询、分页和投影维护 | 各业务服务 `pom.xml`，相关 `Service` |
| MySQL Connector/J | `8.0.33` | 连接各服务数据库，支撑用户、活动、公告、反馈以及 outbox/projection 表 | 各业务服务 `pom.xml`，各服务 `application.properties` |
| RabbitMQ / AMQP | 服务端镜像 `3.13-management-alpine` | 承担领域事件分发、死信队列、异步解耦和本地投影更新 | `services/common-messaging`，`compose.stack.yml` |
| Redis | 服务端镜像 `7-alpine` | 用于网关限流和活动名额库存控制 | `compose.shared.yml`，网关配置，`ActivityService.java` |
| MinIO | Java SDK `8.5.17`，服务端镜像 `latest` | 用于活动图片、公告图片/附件、反馈附件的对象存储 | 各业务服务 `pom.xml`，各 `MinioStorageService` |
| JWT | JJWT `0.11.5` | 用于登录令牌签发、网关鉴权、MCP 访问控制 | `services/common/pom.xml`，`JwtUtil.java`，网关过滤器 |
| AI 接口接入 | DeepSeek OpenAI 兼容接口；Spring AI `1.1.0` 用于 MCP 框架 | 活动服务调用 AI 生成活动文案；MCP 服务使用 Spring AI 提供工具协议能力 | `services/activity-service/src/main/resources/application.properties`，`AIService.java`，`services/mcp-service/pom.xml` |
| Resilience4j | `2.2.0` 或由 BOM 管理 | 用于 AI 调用、跨服务 HTTP 调用、网关路由和 MCP 网关访问的重试/熔断/隔离 | 网关、活动服务、MCP 服务相关配置与代码 |
| Actuator / Admin / Prometheus / OTel | Boot Admin `3.3.4`，Prometheus `2.54.1`，OTel Collector `0.108.0` | 暴露健康检查与指标，汇聚追踪，提供管理控制台 | 各服务配置，`deploy/observability/*` |
| 日志与链路追踪 | Micrometer Tracing / OTel 版本由 Boot 管理 | 通过 `X-Trace-Id`、MDC、Promtail、Loki、Tempo 构成日志与追踪链路 | 网关过滤器、各服务日志过滤器、观测配置 |
| Docker Compose 分层部署 | Nacos `2.4.2`、MySQL `8.0`、Grafana `11.2.0` 等 | 实现共享基础设施、双业务栈和边缘入口的分层部署 | `compose.shared.yml`，`compose.stack.yml`，`compose.edge.yml` |
| MCP 服务 | Spring AI `1.1.0` | 将平台能力封装为 MCP Tools，供外部 Agent 通过标准协议访问 | `services/mcp-service/pom.xml`，工具配置与控制器 |

### 1. Spring Cloud

在本项目中，Spring Cloud 不是停留在依赖管理层，而是承担了真实的服务治理职责。网关路由直接使用 `lb://user-service`、`lb://activity-service` 等服务名转发请求；`activity-service` 通过 `@LoadBalanced RestTemplate` 以 `http://user-service` 的方式调用用户服务内部接口。这说明服务调用方并不依赖固定 IP 或端口，而是依赖注册中心中的服务名完成寻址和负载分发。

Spring Cloud 的另一项实际作用是把弹性治理接入统一入口。网关中每条主要业务路由都绑定了 `CircuitBreaker` 和 `fallbackUri`，这意味着当下游服务不可用时，系统不会直接把错误暴露给前端，而是返回结构化的降级结果。

证据文件：

- `pom.xml`
- `services/gateway-service/src/main/resources/application.properties`
- `services/activity-service/src/main/java/org/example/config/HttpClientConfig.java`
- `services/activity-service/src/main/java/org/example/client/UserServiceClient.java`

### 2. Spring Cloud Alibaba / Nacos

项目中明确落地的是 Nacos Discovery。`gateway-service`、`user-service`、`activity-service`、`announcement-service`、`feedback-service`、`monitor-service` 都启用了 Nacos 注册发现配置，并在启动类上使用了 `@EnableDiscoveryClient`。Compose 中还单独部署了 `nacos/nacos-server:v2.4.2`，说明它是运行时基础设施的一部分，而不是开发期占位依赖。

Nacos 在这里的实际作用主要有两点：一是让网关能根据服务名完成路由；二是让服务之间能通过服务名完成调用，而不需要手工维护地址列表。需要说明的是，代码中没有发现 `spring.cloud.nacos.config`、`spring.config.import=nacos:` 或 `@RefreshScope` 等配置，因此当前仓库只证明使用了“注册发现”，不能证明已经实际使用了“Nacos 配置中心”。

证据文件：

- `services/gateway-service/src/main/java/org/example/GatewayApplication.java`
- `services/user-service/src/main/java/org/example/UserApplication.java`
- `services/activity-service/src/main/resources/application.properties`
- `compose.stack.yml`

### 3. Gateway

网关是本项目微服务架构最典型的落地点之一。它并不只是“把请求转发出去”，而是承担了统一入口层的多项职责。首先，它把 `/api/user/**`、`/api/activity/**`、`/api/announcement/**`、`/api/feedback/**` 分发到不同业务服务，并通过 `RewritePath` 去掉统一前缀。其次，它在 `AuthFilter` 中解析 JWT，对白名单接口放行，对需要登录的接口做令牌校验。校验成功后，网关会把 `X-User-Id`、`X-Username`、`X-User-Role` 注入下游请求，实现身份透传。

此外，网关还接入了两个典型的治理能力。其一是基于 Redis 的请求限流，按用户 ID 或客户端 IP 生成限流 Key；其二是基于 Resilience4j 的路由级熔断和降级，当用户、活动、公告、反馈服务异常时，由 `GatewayFallbackController` 返回统一的 503 降级响应。这说明网关在本项目中已经承担了“入口治理层”的角色，而不是简单反向代理。

证据文件：

- `services/gateway-service/pom.xml`
- `services/gateway-service/src/main/resources/application.properties`
- `services/gateway-service/src/main/java/org/example/filter/AuthFilter.java`
- `services/gateway-service/src/main/java/org/example/config/GatewayResilienceConfig.java`
- `services/gateway-service/src/main/java/org/example/controller/GatewayFallbackController.java`

### 4. Spring MVC / WebFlux

项目在 Web 技术边界上划分得比较清楚。`gateway-service` 依赖的是 `spring-cloud-starter-gateway` 和响应式 Redis，因此入口层采用的是 WebFlux 模型，过滤器实现为 `GlobalFilter`，返回值是 `Mono<Void>`；而 `user-service`、`activity-service`、`announcement-service`、`feedback-service`、`monitor-service` 和 `mcp-service` 都依赖 `spring-boot-starter-web`，并使用 `OncePerRequestFilter`、`@RestController` 等 Servlet MVC 组件。

这种组合方式的实际意义在于：把高并发入口治理放在响应式网关层，而把复杂业务逻辑保留在更熟悉的同步 MVC 模式中。它既体现了技术分层，也降低了业务服务整体改造成响应式的复杂度。

证据文件：

- `services/gateway-service/pom.xml`
- `services/gateway-service/src/main/java/org/example/filter/AuthFilter.java`
- `services/activity-service/pom.xml`
- `services/activity-service/src/main/java/org/example/logging/RequestLoggingFilter.java`

### 5. MyBatis-Plus

MyBatis-Plus 在本项目中是主数据访问框架，而不是备用依赖。四个核心业务服务都引入了 `mybatis-plus-spring-boot3-starter`，启动类使用 `@MapperScan`，配置文件中定义了 mapper 路径和驼峰映射规则。具体业务代码大量使用了 `LambdaQueryWrapper`、`updateById`、`selectById`、`selectCount`、`selectBatchIds` 等 API，这些都是 MyBatis-Plus 的典型实际用法。

从代码风格看，它承担的是“轻量 ORM + 手写业务 SQL”的角色。也就是说，项目没有引入完整的 JPA 领域模型，而是用 MyBatis-Plus 快速完成常规 CRUD，再在需要时保留 mapper 层的可控性，这与教学型或中小型微服务项目的实现习惯是吻合的。

证据文件：

- `services/user-service/src/main/java/org/example/UserApplication.java`
- `services/activity-service/src/main/java/org/example/ActivityApplication.java`
- `services/user-service/src/main/java/org/example/service/UserService.java`
- `services/activity-service/src/main/java/org/example/service/ActivityService.java`

### 6. MySQL

MySQL 在项目中的作用不是单一“数据库”，而是每个业务服务的本地持久化载体。`user-service`、`activity-service`、`announcement-service`、`feedback-service` 在各自配置文件中都指定了独立的数据库名和账号，例如 `user_service_db`、`activity_service_db` 等。数据库迁移脚本还进一步把旧的共享库拆分为多个服务库，并为每个服务建立本地表、outbox 表、消息消费记录表和投影表。

这说明项目已经有意识地朝“数据库按服务拆分”演进。需要指出的是，在部署层面这些数据库目前仍然托管在同一个 MySQL 容器实例中，所以它实现的是“逻辑隔离的多库”，还不是“物理隔离的独立数据库集群”。

证据文件：

- `services/user-service/src/main/resources/application.properties`
- `services/activity-service/src/main/resources/application.properties`
- `database/migrations/20260415_add_messaging_outbox.sql`
- `database/migrations/20260416_split_service_databases.sql`
- `compose.shared.yml`

### 7. RabbitMQ

RabbitMQ 在本项目中并不是“预留能力”，而是已经进入了真实业务链路。`common-messaging` 模块统一定义了事件交换机、死信交换机、业务队列、死信队列以及路由键；`activity-service` 和 `feedback-service` 会把业务事件先写入本地 `event_outbox` 表，再由定时任务发布到 RabbitMQ；`announcement-service` 和 `user-service` 则通过 `@RabbitListener` 消费事件并更新本地投影数据或用户工时。

更重要的是，这套消息机制已经与具体业务绑定。活动服务在创建、更新、删除活动时发布 `activity.upserted` / `activity.deleted` 事件，公告服务消费这些事件维护 `ActivityProjection`；活动服务在确认工时时发布 `user.updated` 事件，用户服务消费后增加志愿时长；反馈服务在创建反馈时发布 `feedback.created` 事件，用户服务消费后维护 `UserFeedbackProjection`。这是一条典型的“异步事件驱动 + 本地投影”微服务实现路径。

证据文件：

- `services/common-messaging/src/main/java/org/example/messaging/RabbitConfig.java`
- `services/common-messaging/src/main/java/org/example/messaging/MessagingConstants.java`
- `services/activity-service/src/main/java/org/example/messaging/ActivityOutboxPublisher.java`
- `services/feedback-service/src/main/java/org/example/messaging/FeedbackOutboxPublisher.java`
- `services/announcement-service/src/main/java/org/example/messaging/ActivityProjectionConsumer.java`
- `services/user-service/src/main/java/org/example/messaging/UserUpdatedConsumer.java`

### 8. Redis

Redis 在项目中有两种明确用途。第一种用途位于网关层，用于 `RequestRateLimiter` 的分布式限流，限制不同业务路由的访问速率；第二种用途位于活动服务中，用于活动库存控制。活动创建时会把可报名名额写入 Redis，用户报名时先对库存做 `decrement`，若库存不足立即回滚并拒绝报名；取消报名时再把库存加回去。

这意味着 Redis 在本项目里承担了“高频实时控制”的职责，而不是单纯缓存。特别是在活动报名场景下，它被用于减少超卖风险，这是一种典型的微服务业务加速与并发保护手段。

证据文件：

- `services/gateway-service/src/main/resources/application.properties`
- `services/gateway-service/src/main/java/org/example/config/GatewayResilienceConfig.java`
- `services/activity-service/src/main/java/org/example/service/ActivityService.java`
- `services/common/src/main/java/org/example/common/constant/RedisKeyConstant.java`

### 9. MinIO

MinIO 在项目中承担统一对象存储角色，且已经被多个业务服务真正使用。`activity-service` 用它管理活动图片，`announcement-service` 用它管理公告图片和附件，`feedback-service` 用它管理反馈附件。各服务的 `MinioStorageService` 都包含上传、读取、删除、桶存在性检查和公开访问 URL 构建逻辑，这说明对象存储已经被纳入业务闭环。

这种做法的好处是把文件类资源从数据库中剥离出来，让公告附件、活动图片和反馈附件都可以通过对象 Key 管理，而业务库只保存元数据或引用关系，更符合微服务系统中“结构化数据与文件对象分离”的常见设计。

证据文件：

- `services/activity-service/pom.xml`
- `services/activity-service/src/main/java/org/example/service/MinioStorageService.java`
- `services/announcement-service/src/main/java/org/example/service/MinioStorageService.java`
- `services/feedback-service/src/main/java/org/example/service/MinioStorageService.java`
- `compose.shared.yml`

### 10. JWT

JWT 在本项目中的使用链路是完整的。用户在 `user-service` 登录成功后，由 `JwtUtil` 生成包含 `userId`、`username`、`role` 的令牌；请求经过网关时，由 `AuthFilter` 校验令牌有效性，并把用户身份信息写入下游请求头；这样，后端服务可以在无需重复解析 JWT 的前提下拿到调用者身份。MCP 服务则进一步复用了平台的 JWT，把它作为 MCP 访问令牌解析和透传。

因此，JWT 在这里不只是“登录后返回一个 token”，而是串起了“用户认证 - 网关鉴权 - 身份透传 - Agent 工具访问”这整条链路。它已经成为系统访问控制的中心凭据。

证据文件：

- `services/common/src/main/java/org/example/common/util/JwtUtil.java`
- `services/user-service/src/main/java/org/example/service/UserService.java`
- `services/gateway-service/src/main/java/org/example/filter/AuthFilter.java`
- `services/mcp-service/src/main/java/org/example/mcp/auth/McpJwtTokenService.java`
- `services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java`

### 11. AI 接口接入

活动服务中的 AI 接入是项目的一个应用型特色。它没有停留在“配置了 API Key”，而是把 AI 能力接进了管理端的活动创建流程。`AIService` 会根据地点、分类、关键词和志愿时长拼接提示词，再以 OpenAI 兼容格式调用 `https://api.deepseek.com/v1/chat/completions`，生成中文活动招募文案。控制器中还提供了 `/activity/ai/generate` 接口，说明这项能力已经暴露给业务使用。

同时，这条链路并不是“硬调用”。如果 API Key 缺失，或者外部模型调用失败，服务会自动回退到本地模板文案；并且整个 AI 调用又被 Resilience4j 的重试、熔断和隔离保护起来。这说明 AI 接入在本项目中属于“可选增强能力”，不会因为外部模型异常而拖垮主业务。

需要区分的是，`activity-service` 的 AI 调用是手写 `RestTemplate` 实现的 DeepSeek/OpenAI 兼容请求；`mcp-service` 中出现的 Spring AI，则主要用于搭建 MCP Server，而不是直接承担活动文案生成。

证据文件：

- `services/activity-service/src/main/resources/application.properties`
- `services/activity-service/src/main/java/org/example/service/AIService.java`
- `services/activity-service/src/main/java/org/example/controller/ActivityController.java`
- `services/mcp-service/pom.xml`

### 12. Resilience4j

Resilience4j 在这个项目里属于“真正接入业务链路”的弹性组件。第一条链路是网关：每条核心路由都配置了熔断器和超时限制，异常时进入统一降级控制器。第二条链路是活动服务：调用 AI 文案接口时用了 `@Retry`、`@CircuitBreaker` 和 `@Bulkhead`；调用用户服务内部接口时则进一步加入了 `@RateLimiter`，避免下游被过量访问。第三条链路是 MCP 服务：所有通过网关访问业务接口的调用都带有 `Retry + CircuitBreaker + Bulkhead` 保护。

这说明项目并不是把 Resilience4j 只写在配置文件里，而是落实到了“入口层保护”“服务间调用保护”“外部依赖保护”三个层面。对于课程项目而言，这种接入深度已经比较完整。

证据文件：

- `services/gateway-service/src/main/resources/application.properties`
- `services/activity-service/src/main/java/org/example/client/UserServiceClient.java`
- `services/activity-service/src/main/java/org/example/service/AIService.java`
- `services/mcp-service/src/main/java/org/example/mcp/gateway/CloudDemoGatewayClient.java`
- `services/gateway-service/src/main/java/org/example/controller/GatewayFallbackController.java`

### 13. Actuator / Admin / Prometheus / OTel

可观测性在本项目中已经形成了比较完整的技术链。各服务配置中都暴露了 `health`、`info` 和 `prometheus` 端点，并启用了 OTLP Trace 输出；`otel-collector` 负责接收追踪数据并转发到 Tempo；Prometheus 通过 Docker 服务发现自动抓取 stack-a、stack-b 和 shared 中各服务的 `/actuator/prometheus` 指标；Grafana 则预配置了 Prometheus、Loki 和 Tempo 三种数据源。

在管理控制台层面，`monitor-service` 提供 Spring Boot Admin Server，并启用了 discovery 模式；`mcp-service` 还显式接入了 Admin Client。也就是说，本项目已经实现了“健康检查 + 指标采集 + 链路追踪 + 监控面板”的多层可观测方案。需要谨慎说明的是，代码中没有直接看到 Alertmanager 或告警规则配置，因此“监控展示”已经落地，“自动告警”暂不能下结论。

证据文件：

- `services/monitor-service/src/main/resources/application.properties`
- `services/mcp-service/src/main/resources/application.properties`
- `deploy/observability/otel-collector-config.yml`
- `deploy/observability/prometheus.yml`
- `deploy/observability/grafana/provisioning/datasources/datasources.yml`

### 14. 日志与链路追踪

项目的日志体系做得比较有工程感。网关会优先读取请求头中的 `X-Trace-Id`，没有则生成新的 traceId，并回写到响应头；业务服务中的 `RequestLoggingFilter` 会把 `traceId`、`stackId`、`serviceName`、`eventType`、`messageId` 写入 MDC；日志格式又统一打印这些字段，因此一条请求从网关进入业务服务后，可以在日志中保留较稳定的链路标识。

在日志汇聚层，Promtail 会从 Docker 容器和 edge 文件日志中采集数据并推送到 Loki；在追踪层，OTel + Tempo 提供 trace 数据。这样，项目形成了“请求头传递 traceId + 本地日志结构化 + 集中式日志检索 + 追踪存储”的联合观测方式。

证据文件：

- `services/gateway-service/src/main/java/org/example/filter/AuthFilter.java`
- `services/activity-service/src/main/java/org/example/logging/RequestLoggingFilter.java`
- `services/mcp-service/src/main/java/org/example/mcp/logging/RequestLoggingFilter.java`
- `deploy/observability/promtail-config.yml`

### 15. Docker Compose 部署

Compose 部署不是简单把所有容器放到一个文件里，而是体现了清晰的分层思想。`compose.shared.yml` 负责共享基础设施和共享 MCP 服务；`compose.stack.yml` 负责业务栈，可以启动 stack-a 和 stack-b 两套业务服务；`compose.edge.yml` 负责边缘入口 Nginx。`nginx.conf` 中甚至实现了基于客户端信息的分流，并支持通过 `X-Stack-Id` 强制命中某一栈，这已经具备了简化版蓝绿或双栈发布的味道。

同时，业务服务在 Compose 中都具有独立端口、独立健康检查、独立日志卷和独立副本数配置，例如 `user-service`、`activity-service`、`announcement-service`、`feedback-service` 都可以分别设置 `scale`。这再次说明它不是单体拆模块，而是可实际分开部署和扩缩容的服务集合。

证据文件：

- `compose.shared.yml`
- `compose.stack.yml`
- `compose.edge.yml`
- `deploy/edge-nginx/nginx.conf`

### 16. MCP 服务

MCP 服务是本项目区别于传统课程项目的一个亮点。它没有直接连接业务数据库，而是通过 `CloudDemoGatewayClient` 调用现有网关接口，再把这些接口封装为 `ActivityMcpTools`、`AnnouncementMcpTools`、`FeedbackMcpTools`、`UserMcpTools`。换句话说，它不是重写一套业务逻辑，而是在现有微服务体系之上增加一个“Agent 访问层”。

为了让外部 Agent 能安全访问，MCP 服务还实现了较完整的授权流程：提供 `/.well-known/oauth-protected-resource`、`/.well-known/oauth-authorization-server`、`/authorize`、`/token`、`/register` 等端点，支持基于授权码和 PKCE 的访问令牌获取；对 `/mcp` 端点，又通过 `McpAccessTokenFilter` 做 Bearer Token 校验。这个实现说明项目已经把微服务能力进一步“协议化”，向 Agent 工具生态开放。

证据文件：

- `services/mcp-service/pom.xml`
- `services/mcp-service/src/main/java/org/example/mcp/config/ToolConfig.java`
- `services/mcp-service/src/main/java/org/example/mcp/gateway/CloudDemoGatewayClient.java`
- `services/mcp-service/src/main/java/org/example/mcp/auth/AuthController.java`
- `services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java`

## 四、项目中微服务架构的落地效果分析

从落地效果看，这个项目已经实现了微服务架构中的几个关键目标。第一，服务可以独立部署。每个服务都有独立启动类、独立 Dockerfile、独立端口、独立健康检查和独立扩容配置。第二，服务能通过注册中心发现彼此，避免地址硬编码。第三，系统具备统一入口，前端和 MCP 工具都优先通过网关进入后端。第四，通信方式是混合型的，既有活动服务对用户服务的同步 HTTP 调用，也有活动、反馈等领域事件的异步 MQ 分发。第五，公共能力确实下沉到了 `common` 和 `common-messaging`，从而降低了重复代码。

更重要的是，这些技术不是彼此孤立地存在，而是形成了完整的业务链路。例如，“管理员确认工时”这一动作，会先在活动服务中修改报名记录，再写入 outbox，随后经 RabbitMQ 把 `user.updated` 事件送到用户服务，由用户服务异步更新志愿时长；这条链路同时使用了 MySQL、MyBatis-Plus、RabbitMQ、outbox、幂等消费和日志追踪。再如，“AI 生成活动文案”同时调用了外部大模型接口和 Resilience4j 保护策略，体现了外部依赖接入的工程化处理。

因此，如果从课程设计的角度评价，这个项目已经不是“把几个模块放在一个仓库里”的伪微服务，而是一个具备真实服务治理、部署拓扑、异步解耦和可观测闭环的微服务实践系统。

证据文件：

- `compose.stack.yml`
- `services/activity-service/src/main/java/org/example/client/UserServiceClient.java`
- `services/activity-service/src/main/java/org/example/service/ActivityService.java`
- `services/user-service/src/main/java/org/example/messaging/UserUpdatedConsumer.java`
- `services/common-messaging/pom.xml`

## 五、当前不足与优化建议

第一，配置中心没有真正落地。代码能证明项目使用了 Nacos Discovery，但看不到 Nacos Config 的接入配置，因此服务配置仍主要依赖本地 `application.properties` 和环境变量。后续可以把数据库、消息队列、AI 接口、JWT 密钥等配置迁移到统一配置中心。

第二，基础设施层存在明显共享单点。虽然业务数据库已经按服务拆库，但在运行时仍共享同一个 MySQL 容器；Redis、RabbitMQ、MinIO、Nacos 也都是单实例。这对课程项目没有问题，但若面向生产，需要进一步做高可用和故障隔离。

第三，安全链路还有增强空间。`JwtUtil` 中存在硬编码密钥，而各服务配置文件中的 JWT 密钥又不完全一致；同时，下游服务大量依赖网关注入的 `X-User-Id`、`X-Username`、`X-User-Role` 头部，这意味着内部网络默认被信任。更稳妥的做法是统一密钥配置，并在内部服务侧增加签名校验或零信任网关方案。

第四，监控链路已经具备展示能力，但运维治理还不够完整。项目已经有 Actuator、Prometheus、Grafana、Loki、Tempo 和 Boot Admin，但代码中未看到告警规则、告警路由或自动恢复机制。后续可以增加 Alertmanager、SLO 指标和关键业务链路告警。

第五，消息模型还有未完全用到的部分。`announcement.published` 的交换机路由和队列常量已经定义，但在当前主代码中没有看到明确的发布者和消费者链路，这意味着消息模型设计略超前于现有落地范围。

第六，版本管理上存在轻微分裂。主体服务使用 Spring Boot `3.3.4`，而 `mcp-service` 独立使用 Spring Boot `3.4.2`。这种做法在当前规模下问题不大，但长期会增加依赖治理和兼容性验证成本。

证据文件：

- `services/common/src/main/java/org/example/common/util/JwtUtil.java`
- `services/user-service/src/main/resources/application.properties`
- `services/gateway-service/src/main/resources/application.properties`
- `services/common-messaging/src/main/java/org/example/messaging/MessagingConstants.java`
- `services/mcp-service/pom.xml`

## 六、结论

综合代码与配置分析可以得出结论：`cloud-demo` 已经较完整地把微服务架构中的核心技术落地到了真实业务系统中。它不仅完成了服务拆分，还实现了注册发现、统一网关、同步与异步通信、分库持久化、对象存储、JWT 认证、AI 接口接入、弹性保护、日志追踪、指标采集和分层部署。尤其是活动、公告、反馈、用户之间通过 outbox 和消息投影形成的协作关系，使这个项目具备了明显的微服务实践特征。

同时，这个项目也体现出典型的“教学型微服务平台”特征：它强调技术链路完整、功能闭环清晰，但在配置中心、基础设施高可用、统一安全治理和自动告警方面还有继续深化的空间。也正因为如此，它非常适合作为课程报告或答辩案例，因为它既展示了微服务技术的实际使用方式，又保留了可以继续优化和讨论的工程问题。
