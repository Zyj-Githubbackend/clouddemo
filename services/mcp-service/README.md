# mcp-service

`mcp-service` 是本项目的独立 MCP Server 模块，用于把现有校园志愿服务平台能力暴露给支持 MCP 的客户端。

## 1. 基本信息

- 服务名：`mcp-service`
- 端口：`9300`
- 协议：Streamable HTTP
- MCP 端点：`/mcp`

默认上游网关地址：

```text
http://127.0.0.1:9000
```

可通过环境变量覆盖：

```text
CLOUD_DEMO_API_BASE_URL
```

## 2. 当前能力

### OAuth 相关端点

- `/.well-known/oauth-protected-resource`
- `/.well-known/oauth-authorization-server`
- `/authorize`
- `/token`
- `/register`

### 辅助登录端点

- `POST /mcp/auth/login`

### MCP 工具

普通用户常用工具：

- `listActivities`
- `getActivityDetail`
- `listHomeAnnouncements`
- `listAnnouncements`
- `getAnnouncementDetail`
- `getMyRegistrations`
- `registerActivity`
- `cancelMyRegistration`
- `getMyProfile`
- `getMyVolunteerSummary`
- `updateMyProfile`
- `updateMyPassword`
- `exportMyConfirmedRegistrations`
- `createFeedback`
- `listMyFeedback`
- `getFeedbackDetail`
- `replyMyFeedback`
- `closeMyFeedback`
- `uploadFeedbackAttachment`
- `downloadFeedbackAttachment`

管理员工具：

- `generateActivityDescription`
- `uploadActivityImage`
- `createActivity`
- `updateActivity`
- `cancelActivity`
- `completeActivity`
- `deleteActivity`
- `listAdminRegistrations`
- `listPendingCheckInRegistrations`
- `listEndedActivities`
- `listCheckInActivities`
- `checkInRegistration`
- `confirmHours`
- `batchConfirmHours`
- `listVolunteerHours`
- `listAdminAnnouncements`
- `getAdminAnnouncementDetail`
- `createAnnouncement`
- `updateAnnouncement`
- `publishAnnouncement`
- `offlineAnnouncement`
- `deleteAnnouncement`
- `uploadAnnouncementImage`
- `uploadAnnouncementAttachment`
- `listAdminFeedback`
- `getAdminFeedbackDetail`
- `replyFeedbackAsAdmin`
- `closeFeedbackAsAdmin`
- `rejectFeedbackAsAdmin`
- `updateFeedbackPriority`

公告工具说明：

- `listHomeAnnouncements`、`listAnnouncements`、`getAnnouncementDetail`、`getAdminAnnouncementDetail` 会返回 `activityIds`、`activities` 和 `attachments`。
- `createAnnouncement`、`updateAnnouncement` 兼容旧的 `activityId`，也支持 `activityIdsCsv` 关联多个活动；附件可通过 `attachmentsJson` 或 `attachmentKeysCsv` 传入。

意见反馈工具说明：

- `createFeedback`、`replyMyFeedback`、`replyFeedbackAsAdmin` 可通过 `attachmentsJson` 绑定上传后的附件。
- `uploadFeedbackAttachment` 支持 JPG、PNG、GIF、WEBP、PDF、Excel、Word、TXT 和 CSV。
- `listAdminFeedback` 支持按 `status`、`category`、`priority` 和 `keyword` 筛选。
- `downloadFeedbackAttachment` 会复用业务权限校验，普通用户只能访问自己的反馈附件。

补充工具：

- `registerUser`

## 3. 启动方式

```bash
mvn -pl services/mcp-service -am -DskipTests compile
mvn -pl services/mcp-service spring-boot:run
```

## 4. 认证说明

- `/mcp` 路径默认要求 `Authorization: Bearer <token>`
- 推荐通过 OAuth 授权码方式登录
- 手动调试时也可以先调用 `/mcp/auth/login` 获取 token
- 管理员工具要求业务 JWT 中 `role=ADMIN`

说明：

- `mcp-service` 复用业务系统现有 JWT，而不是引入一套完全独立的用户体系
- 因为 `/mcp` 整体由 `McpAccessTokenFilter` 保护，正常的 MCP 客户端使用流程应先登录再调用工具

## 5. 关键环境变量

- `APP_LOG_FILE`
- `CLOUD_DEMO_API_BASE_URL`
- `CLOUD_DEMO_API_CONNECT_TIMEOUT`
- `CLOUD_DEMO_API_READ_TIMEOUT`
- `CLOUD_DEMO_JWT_SECRET`
- `MONITOR_SERVICE_URL`
- `MCP_SERVICE_BASE_URL`

## 6. 监控接入

当前模块已启用 Spring Boot Admin Client：

- 默认监控地址：`http://127.0.0.1:9100`
- 默认实例名：`mcp-service`

## 7. 日志

当前模块已对齐其他后端模块的日志配置：

- 默认本机日志文件：按运行方式决定，可通过 `APP_LOG_FILE` 覆盖
- Docker 日志文件：`/app/logs/debug.log`
- Docker 单栈挂载目录：`log/docker/mcp-service/`
- 日志包含 `traceId`、请求方法、路径、状态码、耗时、用户信息与远端 IP

可通过 `APP_LOG_FILE` 覆盖日志文件位置。

## 8. 本机与 Docker 对外地址

本机 Nginx 模式：

```text
http://localhost/mcp
```

Docker 模式：

```text
http://localhost:8081/mcp
```

## 9. 相关文件

- `src/main/java/org/example/mcp/tool/ActivityMcpTools.java`
- `src/main/java/org/example/mcp/tool/AnnouncementMcpTools.java`
- `src/main/java/org/example/mcp/tool/FeedbackMcpTools.java`
- `src/main/java/org/example/mcp/tool/UserMcpTools.java`
- `src/main/java/org/example/mcp/auth/AuthController.java`
- `src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java`
- `src/main/java/org/example/mcp/logging/RequestLoggingFilter.java`
- `src/main/resources/application.properties`
