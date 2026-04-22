# MCP 连接说明

本文档说明如何将本项目的 `mcp-service` 接入支持 MCP 的客户端，例如 Codex CLI。

## 1. 目标与地址

`services/mcp-service` 提供独立 MCP Server，协议为 Streamable HTTP。

推荐地址：

- 本机 Nginx 模式：`http://localhost/mcp`
- Docker 模式：`http://localhost:8081/mcp`

OAuth 相关端点：

- `/.well-known/oauth-protected-resource`
- `/.well-known/oauth-authorization-server`
- `/authorize`
- `/token`
- `/register`

说明：

- `mcp-service` 自身不重复实现业务逻辑，而是转调现有网关接口
- 管理员权限仍然由业务 JWT 的 `role=ADMIN` 决定
- 当前 `/mcp` 路径受 Bearer Token 保护，因此实际使用时应先完成 MCP 登录

## 2. 启动方式

### Docker 单栈

```powershell
powershell -ExecutionPolicy Bypass -File deploy/docker/up.ps1
```

MCP 地址：

- `http://localhost:8081/mcp`

如果已经整套环境启动完毕，只重建 `mcp-service` 时可使用：

```powershell
docker compose -p cloud-demo -f deploy/docker/docker-compose.yml up -d --build mcp-service
```

说明：

- `mcp-service` 默认上游业务网关是 `http://gateway-service:9000`
- 运行期日志位于 `log/docker/mcp-service/debug.log`

### 本机 Java + 本机 Nginx

```powershell
mvn -pl services/mcp-service -am -DskipTests compile
mvn -pl services/mcp-service spring-boot:run
```

MCP 地址：

- `http://localhost/mcp`

## 3. 连接前验证

### OAuth 元数据

Docker 模式：

```powershell
curl.exe "http://localhost:8081/.well-known/oauth-protected-resource"
curl.exe "http://localhost:8081/.well-known/oauth-authorization-server"
```

本机 Nginx 模式：

```powershell
curl.exe "http://localhost/.well-known/oauth-protected-resource"
curl.exe "http://localhost/.well-known/oauth-authorization-server"
```

重点确认返回中的 `issuer`、`authorization_endpoint`、`token_endpoint`、`registration_endpoint` 是否带有正确端口。

### 未登录访问 `/mcp`

```powershell
curl.exe -i "http://localhost/mcp"
```

或：

```powershell
curl.exe -i "http://localhost:8081/mcp"
```

预期结果：

- 返回 `401 Unauthorized`
- 响应头包含 `WWW-Authenticate`

这说明 MCP 保护与 OAuth 引导已生效。

### Streamable HTTP 调试提醒

手动调试 `/mcp` 时建议注意两点：

1. `Accept` 建议同时带上 `text/event-stream, application/json`
2. 某些工具失败不会返回顶层 JSON-RPC `error`，而是返回 `HTTP 200 + result.isError=true`

也就是说，客户端或调试脚本除了检查 HTTP 状态码，还需要检查返回体中的 `result.isError`。

### 辅助登录接口

如果要先验证账号密码是否可用：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost/mcp/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'
```

Docker 模式下把地址换成 `http://localhost:8081/mcp/auth/login`。

返回字段包括：

- `token`
- `role`
- `username`
- `mcpEndpoint`

## 4. Codex CLI 接入

### 添加 MCP 服务

本机 Nginx 模式：

```powershell
codex mcp add cloud-demo --url http://localhost/mcp
```

Docker 模式：

```powershell
codex mcp add cloud-demo --url http://localhost:8081/mcp
```

查看配置：

```powershell
codex mcp list
codex mcp get cloud-demo
```

### OAuth 登录

```powershell
codex mcp login cloud-demo
```

执行后通常会：

1. 打开浏览器
2. 跳到本项目的 `/authorize` 页面
3. 使用平台账号登录
4. 换取 access token
5. 后续自动带 token 调用 `/mcp`

默认测试账号：

- 管理员：`admin / password123`
- 志愿者：`student01 / password123`

## 5. 手动 token 备用方案

如果暂时不想走浏览器 OAuth，可使用脚本把 token 写入环境变量：

```powershell
.\scripts\mcp-login.ps1 -McpBaseUrl http://localhost -Username admin -Password password123
```

Docker 模式：

```powershell
.\scripts\mcp-login.ps1 -McpBaseUrl http://localhost:8081 -Username admin -Password password123
```

脚本会把 token 写入环境变量 `CLOUD_DEMO_MCP_BEARER_TOKEN`，随后可用：

```powershell
codex mcp add cloud-demo-manual --url http://localhost/mcp --bearer-token-env-var CLOUD_DEMO_MCP_BEARER_TOKEN
```

如果只是想临时获取并打印 token，可使用：

```powershell
.\scripts\mcp-print-token.ps1 -McpBaseUrl http://localhost -Username admin -Password password123
```

Docker 模式：

```powershell
.\scripts\mcp-print-token.ps1 -McpBaseUrl http://localhost:8081 -Username admin -Password password123
```

默认只输出 token，便于复制或管道处理；如需同时查看账号与端点信息，可加 `-ShowMetadata`：

```powershell
.\scripts\mcp-print-token.ps1 -McpBaseUrl http://localhost -Username admin -Password password123 -ShowMetadata
```

注意：打印出来的 token 等同于登录凭证，只用于本机调试，不要提交到代码仓库或分享给他人。

## 6. Trae IDE 接入

Trae 支持手动添加 MCP Server，也支持项目级 `.trae/mcp.json`。本项目的 MCP 服务是 Streamable HTTP，并且 `/mcp` 需要 Bearer Token，因此配置时需要带上 `Authorization` 请求头。

先获取 token：

本机 Nginx 模式：

```powershell
$token = .\scripts\mcp-print-token.ps1 -McpBaseUrl http://localhost -Username admin -Password password123
```

Docker 模式：

```powershell
$token = .\scripts\mcp-print-token.ps1 -McpBaseUrl http://localhost:8081 -Username admin -Password password123
```

### 方式一：Trae 界面手动添加

在 Trae 中进入 MCP 设置页，选择手动添加 MCP Server，然后粘贴以下配置。如果当前 Trae 版本要求选择传输类型，选择 `Streamable HTTP`。

本机 Nginx 模式：

```json
{
  "mcpServers": {
    "cloud-demo": {
      "url": "http://localhost/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

Docker 模式：

```json
{
  "mcpServers": {
    "cloud-demo": {
      "url": "http://localhost:8081/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

把 `YOUR_TOKEN` 替换为上一步打印出的 token。`Bearer` 和 token 中间只保留一个空格。

### 方式二：项目级 `.trae/mcp.json`

如果希望只在当前项目中启用，可以在 Trae 中启用 Project MCP 后，在仓库根目录创建 `.trae/mcp.json`，内容同上。

注意：

- `.trae/mcp.json` 会包含登录凭证，已在 `.gitignore` 中忽略
- 不要把真实 token 写入可提交的文档、截图或示例配置
- 如果 Trae 保存配置后没有立即生效，可重启 Trae 或重新打开当前项目

验证时可以在 Trae Chat 中询问：

```text
通过 cloud-demo MCP 列出最近 5 个志愿活动
```

如果当前 token 来自管理员账号，也可以尝试管理员工具；如果来自普通用户账号，管理员工具会被业务权限拦截。

## 7. 当前工具清单

### 普通用户常用工具

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

### 管理员工具

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

### 额外工具

- `registerUser`

补充说明：

- `registerUser` 的实现已存在，但当前 `/mcp` 整体受 Bearer Token 保护，因此在实际 Codex CLI 使用中，仍建议先完成 MCP 登录
- 普通用户即使知道管理员工具名，也无法越权调用

### 意见反馈工具说明

- 分类 `category` 支持 `QUESTION`、`SUGGESTION`、`BUG`、`COMPLAINT`、`OTHER`。
- 状态 `status` 支持 `OPEN`、`REPLIED`、`CLOSED`、`REJECTED`。
- 优先级 `priority` 支持 `LOW`、`NORMAL`、`HIGH`、`URGENT`。
- `uploadFeedbackAttachment` 返回的 `attachment` 对象可直接序列化为 `attachmentsJson`，传给 `createFeedback`、`replyMyFeedback` 或 `replyFeedbackAsAdmin`。
- `downloadFeedbackAttachment` 会复用业务权限校验：管理员可下载任意反馈附件，普通用户只能下载自己反馈下的附件。

## 8. 推荐验证顺序

1. `curl.exe "http://localhost/.well-known/oauth-authorization-server"`
2. `curl.exe -i "http://localhost/mcp"`
3. `Invoke-RestMethod -Method Post -Uri "http://localhost/mcp/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'`
4. `codex mcp add cloud-demo --url http://localhost/mcp`
5. `codex mcp login cloud-demo`
6. 登录后尝试查询：`请通过 cloud-demo MCP 列出最近 5 个志愿活动`
7. 反馈工具可用性验证：`创建一条测试反馈，读取详情，追加回复，然后关闭它`

Docker 模式下只需把 `localhost` 替换为 `localhost:8081`。

已验证的反馈 MCP 调用链：

- `listMyFeedback`
- `createFeedback`
- `getFeedbackDetail`
- `replyMyFeedback`
- `closeMyFeedback`
- `listAdminFeedback`
- `getAdminFeedbackDetail`

## 9. 常见问题

### `Registration failed: error sending request for url (http://localhost/register)`

原因通常是 OAuth 元数据里的 `issuer` 或 `registration_endpoint` 没带外部端口。

建议检查：

```powershell
curl.exe "http://localhost:8081/.well-known/oauth-authorization-server"
```

若返回里缺少 `:8081`，说明前端 Nginx 还没加载最新配置，可重建：

```powershell
docker compose -p cloud-demo -f deploy/docker/docker-compose.yml up -d --build edge-nginx
```

### `/mcp` 返回 401

这是正常现象，表示服务要求先完成 OAuth 登录。

### `/mcp/auth/login` 返回 400

建议在 PowerShell 中使用：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost/mcp/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'
```

不要直接照搬 Linux 下多行 `curl` 写法。

### 登录成功后为什么有些工具能用，有些不能用

原因通常是当前账号不是管理员。管理员工具依赖 JWT 中的 `role=ADMIN`。

### Trae 中提示鉴权失败或连接不上

建议检查：

- `Authorization` 请求头是否为 `Bearer <token>` 格式
- token 是否过期，过期后重新执行 `mcp-print-token.ps1` 获取
- URL 是否匹配当前运行模式：本机 Nginx 用 `http://localhost/mcp`，Docker 用 `http://localhost:8081/mcp`
- MCP 服务是否已启动，且 `curl.exe -i "http://localhost/mcp"` 未登录时能返回 `401 Unauthorized`

## 10. 相关文件

- [services/mcp-service/README.md](../../services/mcp-service/README.md)
- [services/mcp-service/src/main/java/org/example/mcp/tool/ActivityMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/ActivityMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/tool/AnnouncementMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/AnnouncementMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/tool/FeedbackMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/FeedbackMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/tool/UserMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/UserMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/auth/AuthController.java](../../services/mcp-service/src/main/java/org/example/mcp/auth/AuthController.java)
- [services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java](../../services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java)
- [services/mcp-service/src/main/resources/application.properties](../../services/mcp-service/src/main/resources/application.properties)
- [deploy/local/nginx/cloud-demo.local.conf](../../deploy/local/nginx/cloud-demo.local.conf)
- [frontend2/nginx.docker.conf](../../frontend2/nginx.docker.conf)
- [scripts/mcp-login.ps1](../../scripts/mcp-login.ps1)
- [scripts/mcp-print-token.ps1](../../scripts/mcp-print-token.ps1)
