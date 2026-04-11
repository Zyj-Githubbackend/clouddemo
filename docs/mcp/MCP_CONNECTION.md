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

### Docker Compose

```powershell
docker compose up -d --build
```

MCP 地址：

- `http://localhost:8081/mcp`

只重建 MCP 相关组件时可使用：

```powershell
docker compose up -d --build mcp-service frontend
```

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

如果暂时不想走浏览器 OAuth，可使用脚本：

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

## 6. 当前工具清单

### 普通用户常用工具

- `listActivities`
- `getActivityDetail`
- `getMyRegistrations`
- `registerActivity`
- `cancelMyRegistration`
- `getMyProfile`
- `getMyVolunteerSummary`
- `updateMyProfile`
- `updateMyPassword`
- `exportMyConfirmedRegistrations`

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

### 额外工具

- `registerUser`

补充说明：

- `registerUser` 的实现已存在，但当前 `/mcp` 整体受 Bearer Token 保护，因此在实际 Codex CLI 使用中，仍建议先完成 MCP 登录
- 普通用户即使知道管理员工具名，也无法越权调用

## 7. 推荐验证顺序

1. `curl.exe "http://localhost/.well-known/oauth-authorization-server"`
2. `curl.exe -i "http://localhost/mcp"`
3. `Invoke-RestMethod -Method Post -Uri "http://localhost/mcp/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'`
4. `codex mcp add cloud-demo --url http://localhost/mcp`
5. `codex mcp login cloud-demo`
6. 登录后尝试查询：`请通过 cloud-demo MCP 列出最近 5 个志愿活动`

Docker 模式下只需把 `localhost` 替换为 `localhost:8081`。

## 8. 常见问题

### `Registration failed: error sending request for url (http://localhost/register)`

原因通常是 OAuth 元数据里的 `issuer` 或 `registration_endpoint` 没带外部端口。

建议检查：

```powershell
curl.exe "http://localhost:8081/.well-known/oauth-authorization-server"
```

若返回里缺少 `:8081`，说明前端 Nginx 还没加载最新配置，可重建：

```powershell
docker compose up -d --build frontend
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

## 9. 相关文件

- [services/mcp-service/README.md](../../services/mcp-service/README.md)
- [services/mcp-service/src/main/java/org/example/mcp/tool/ActivityMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/ActivityMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/tool/UserMcpTools.java](../../services/mcp-service/src/main/java/org/example/mcp/tool/UserMcpTools.java)
- [services/mcp-service/src/main/java/org/example/mcp/auth/AuthController.java](../../services/mcp-service/src/main/java/org/example/mcp/auth/AuthController.java)
- [services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java](../../services/mcp-service/src/main/java/org/example/mcp/auth/McpAccessTokenFilter.java)
- [services/mcp-service/src/main/resources/application.properties](../../services/mcp-service/src/main/resources/application.properties)
- [deploy/nginx/cloud-demo.local.conf](../../deploy/nginx/cloud-demo.local.conf)
- [frontend/nginx.docker.conf](../../frontend/nginx.docker.conf)
- [scripts/mcp-login.ps1](../../scripts/mcp-login.ps1)
