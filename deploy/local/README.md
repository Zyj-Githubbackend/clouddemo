# 本机部署

本目录记录不使用容器编排的本机部署方式：本机运行 MySQL、Redis、Nacos、MinIO、各 Spring Boot 服务和前端，使用本机 Nginx 统一入口。

## 基础组件

- MySQL 8：执行 `database/init.sql`
- Redis：默认 `127.0.0.1:6379`
- Nacos：默认 `127.0.0.1:8848`
- MinIO：API `127.0.0.1:9005`，Console `127.0.0.1:9006`

## 后端启动顺序

1. `UserApplication`
2. `ActivityApplication`
3. `AnnouncementApplication`
4. `FeedbackApplication`
5. `GatewayApplication`
6. `MonitorApplication`
7. `McpApplication`

## 前端

```bash
cd frontend2
npm install
npm run build
```

Nginx 配置文件：

- `deploy/local/nginx/cloud-demo.local.conf`

默认访问：

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`
