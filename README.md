# 校园志愿服务管理平台

基于 Spring Boot 3、Spring Cloud Alibaba、Vue 3、Nginx 与 MySQL 的校园志愿服务管理平台，支持志愿活动发布、报名、签到、时长核销、活动图片管理、个人志愿足迹导出，以及通过独立 `mcp-service` 对外暴露 MCP 能力。

## 1. 项目概览

- 前端：Vue 3 + Vite + Element Plus
- 网关：`gateway-service`，端口 `9000`
- 用户服务：`user-service`，端口 `8100`
- 活动服务：`activity-service`，端口 `8200`
- 公告服务：`announcement-service`，端口 `8300`
- 监控服务：`monitor-service`，端口 `9100`
- MCP 服务：`mcp-service`，端口 `9300`
- 注册中心：Nacos，端口 `8848`
- 数据库：MySQL，库名 `volunteer_platform`
- 缓存：Redis
- 图片存储：MinIO

当前版本的重点能力：

- 活动支持多图上传、编辑和详情轮播展示
- 公告支持独立微服务、首页展示、图片上传和关联活动详情页
- 用户可在“我的志愿足迹”页面取消未开始活动的报名
- 用户可导出本人已核销志愿时长及对应活动明细 Excel
- 管理员可进行活动创建、编辑、取消、结项、签到、时长核销
- 新增独立 `mcp-service`，支持 OAuth 授权码登录与 Streamable HTTP MCP 接入

## 2. 仓库结构

源码与文档主要分布如下：

- `services/`：后端 Maven 聚合工程
- `frontend/`：Vue 3 前端工程
- `database/init.sql`：数据库初始化与示例数据
- `deploy/nginx/cloud-demo.local.conf`：本机 Nginx 配置
- `docker-compose.yml`：整套容器部署方案
- `docs/`：分主题文档中心
- `scripts/mcp-login.ps1`：MCP 手动 token 登录并写入环境变量的辅助脚本
- `scripts/mcp-print-token.ps1`：MCP 手动 token 获取并打印的辅助脚本

另外，仓库根目录下的 `user-service/`、`activity-service/`、`announcement-service/`、`gateway-service/`、`monitor-service/`、`mcp-service/` 目录当前用于保存运行日志，不是源码目录。

## 3. 本机运行摘要

### 3.1 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

注意：

- `database/init.sql` 会先执行 `DROP DATABASE IF EXISTS volunteer_platform`
- 脚本会初始化默认账号、20 条活动数据以及报名记录
- 种子数据围绕 `2026-03-25` 设计，因此不同日期运行时，活动“招募中/未开始/已结束/进行中”的展示会随当前时间动态变化

### 3.2 启动基础设施

- Redis
- Nacos
- MinIO

`activity-service` 默认使用以下 MinIO 配置，本机不改端口时通常无需额外配置：

```powershell
$env:MINIO_ENDPOINT="http://127.0.0.1:9005"
$env:MINIO_ACCESS_KEY="root"
$env:MINIO_SECRET_KEY="12345678"
$env:MINIO_BUCKET="activity-images"
```

### 3.3 编译并启动后端

```bash
mvn clean install -DskipTests
```

启动顺序建议：

1. `UserApplication`
2. `ActivityApplication`
3. `GatewayApplication`
4. `AnnouncementApplication`
5. `MonitorApplication`
6. `McpApplication`

### 3.4 启动前端

开发模式：

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
cd frontend
npm install
npm run build
```

## 4. 访问地址

### 4.1 本机 Nginx 模式

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`
- 开发模式前端：`http://localhost:3000`

Nginx 路由约定：

- `/` -> `frontend/dist`
- `/api/` -> `http://127.0.0.1:9000/`
- `/monitor/` -> `http://127.0.0.1:9100/`
- `/.well-known/*`、`/authorize`、`/token`、`/register`、`/mcp*` -> `http://127.0.0.1:9300`

### 4.2 Docker Compose 模式

启动命令：

```bash
docker compose up -d --build
```

默认访问地址：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- 网关直连：`http://localhost:9001`
- 监控直连：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO 控制台：`http://localhost:9008`

## 5. MCP 接入摘要

`services/mcp-service` 提供独立 MCP Server，协议为 Streamable HTTP。

推荐连接方式：

```powershell
codex mcp add cloud-demo --url http://localhost/mcp
codex mcp login cloud-demo
```

Docker 模式下把地址换成：

```powershell
codex mcp add cloud-demo --url http://localhost:8081/mcp
codex mcp login cloud-demo
```

当前文档已覆盖：

- OAuth 元数据验证
- `codex mcp add` / `codex mcp login`
- 手动 token 备用方案
- 用户工具与管理员工具清单

详见 [docs/mcp/MCP_CONNECTION.md](docs/mcp/MCP_CONNECTION.md)。

## 6. 默认测试账号

`database/init.sql` 内置以下账号，密码均为 `password123`：

| 角色 | 用户名 | 说明 |
|------|------|------|
| 管理员 | `admin` | 可发布活动、签到、核销时长、查看统计 |
| 志愿者 | `student01` - `student10` | 用于普通用户流程测试 |

## 7. 文档导航

- [文档中心](docs/README.md)
- [快速开始](docs/setup/QUICKSTART.md)
- [部署说明](docs/deploy/DEPLOY.md)
- [架构说明](docs/architecture/ARCHITECTURE.md)
- [API 测试](docs/testing/API_TEST.md)
- [MCP 连接说明](docs/mcp/MCP_CONNECTION.md)
- [目录结构](docs/overview/DIRECTORY_STRUCTURE.md)
- [项目交付摘要](docs/overview/PROJECT_SUMMARY.md)
- [验收清单](docs/setup/CHECKLIST.md)
- [前端快速启动](docs/frontend/QUICKSTART.md)
- [前端交付摘要](docs/frontend/PROJECT_COMPLETE.md)

## 8. 推荐阅读顺序

1. `README.md`
2. [docs/setup/QUICKSTART.md](docs/setup/QUICKSTART.md)
3. [docs/deploy/DEPLOY.md](docs/deploy/DEPLOY.md)
4. [docs/testing/API_TEST.md](docs/testing/API_TEST.md)
5. [docs/mcp/MCP_CONNECTION.md](docs/mcp/MCP_CONNECTION.md)
