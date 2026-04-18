# 校园志愿服务管理平台

基于 Spring Boot 3、Spring Cloud Alibaba、Vue 3、Nginx 与 MySQL 的校园志愿服务管理平台，支持志愿活动发布、报名、签到、时长核销、活动图片管理、公告管理、意见反馈工单、个人志愿足迹导出，以及通过独立 `mcp-service` 对外暴露 MCP 能力。

## 1. 项目概览

- 前端：Vue 3 + Vite + Element Plus
- 网关：`gateway-service`，端口 `9000`
- 用户服务：`user-service`，端口 `8100`
- 活动服务：`activity-service`，端口 `8200`
- 公告服务：`announcement-service`，端口 `8300`
- 意见反馈服务：`feedback-service`，端口 `8400`
- 监控服务：`monitor-service`，端口 `9100`
- MCP 服务：`mcp-service`，端口 `9300`
- 注册中心：Nacos，端口 `8848`
- 数据库：MySQL，库名 `volunteer_platform`
- 缓存：Redis
- 图片存储：MinIO

当前版本的重点能力：

- 活动支持多图上传、编辑和详情轮播展示
- 公告支持独立微服务、首页展示、图片上传、附件上传和多活动关联
- 意见反馈支持用户提交、追加回复、附件上传、关闭工单，以及管理员回复、驳回、关闭和优先级调整
- 用户可在“我的志愿足迹”页面取消未开始活动的报名
- 用户可导出本人已核销志愿时长及对应活动明细 Excel
- 管理员可进行活动创建、编辑、取消、结项、签到、时长核销
- 独立 `mcp-service` 支持 OAuth 授权码登录与 Streamable HTTP MCP 接入，工具覆盖活动、用户、公告和意见反馈场景
- 剩余同步高风险链路已接入 Resilience4j（重试、熔断、隔离）
- 已接入完整基础可观测栈：Prometheus、Grafana、Loki、Promtail、Tempo、OpenTelemetry Collector
- Prometheus 与 Loki 已支持按容器实例观察 A/B 栈副本

## 2. 仓库结构

源码、部署文件与运行日志当前主要分布如下：

- `services/`：后端 Maven 聚合工程
- `frontend2/`：Vue 3 前端工程
- `database/init.sql`：数据库初始化与示例数据
- `database/migrations/`：已有数据库的增量升级脚本
- `deploy/`：A/B 双栈部署脚本、环境变量文件、edge nginx 构建文件、本机 Nginx 配置、`bootstrap-db.sql`（Docker 一键部署数据库引导与测试数据）
- `compose.shared.yml`：shared 层（`mysql`、`redis`、`minio`、`mcp-service` 以及 Prometheus / Grafana / Loki / Tempo / OTel / Promtail 等观测组件）
- `compose.stack.yml`：A/B 栈通用 compose（通过 `--env-file` 区分 stack-a / stack-b）
- `compose.edge.yml`：统一入口 `edge-nginx`
- `docker-compose.yml`：历史单架构 compose，保留作兼容参考，不是当前推荐部署入口
- `log/`：Docker 运行期日志目录（`shared/`、`a/`、`b/`、`edge/`）
- `docs/`：分主题文档中心
- `scripts/mcp-login.ps1`：MCP 手动 token 登录并写入环境变量的辅助脚本
- `scripts/mcp-print-token.ps1`：MCP 手动 token 获取并打印的辅助脚本

## 3. 本机运行摘要

### 3.1 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

注意：

- `database/init.sql` 会先执行 `DROP DATABASE IF EXISTS volunteer_platform`
- 脚本会初始化默认账号、20 条活动数据以及报名记录
- 种子数据围绕 `2026-03-25` 设计，因此不同日期运行时，活动“招募中/未开始/已结束/进行中”的展示会随当前时间动态变化
- 已有数据库可按需执行 `database/migrations/` 下的增量脚本（含分库迁移、消息表补齐、活动/反馈投影补建）

### 3.2 启动基础设施

- Redis
- Nacos
- MinIO

`activity-service`、`announcement-service` 和 `feedback-service` 默认使用以下 MinIO 配置，本机不改端口时通常无需额外配置：

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
3. `AnnouncementApplication`
4. `FeedbackApplication`
5. `GatewayApplication`
6. `MonitorApplication`
7. `McpApplication`

### 3.4 启动前端

开发模式：

```bash
cd frontend2
npm install
npm run dev
```

生产构建：

```bash
cd frontend2
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

- `/` -> `frontend2/dist`
- `/api/` -> `http://127.0.0.1:9000/`
- `/monitor/` -> `http://127.0.0.1:9100/`
- `/.well-known/*`、`/authorize`、`/token`、`/register`、`/mcp*` -> `http://127.0.0.1:9300`

### 4.2 Docker A/B 双栈模式

推荐启动命令：

```bash
bash deploy/deploy-all.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/deploy-all.ps1
```

如果只需要按阶段启动，可分别执行：

- `deploy/up-shared.*`
- `deploy/up-stack-a.*`
- `deploy/up-stack-b.*`
- `deploy/up-edge.*`

默认访问地址：

- 前台：`http://localhost:8081/`
- 监控后台 A：`http://localhost:8081/monitor/a/`
- 监控后台 B：`http://localhost:8081/monitor/b/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

说明：

- Docker A/B 模式默认只暴露 `edge-nginx` 的 `8081` 入口，不再直接对宿主机暴露网关、监控、Nacos、MinIO 等内部端口
- 容器运行期日志统一挂载到仓库根目录 `log/`
- A/B 栈中 `user-service`、`activity-service`、`announcement-service`、`feedback-service` 的默认副本数已声明在 `compose.stack.yml` + `deploy/stack-a.env` / `deploy/stack-b.env`，普通重建不会再缩回单实例
- `deploy-all.*` 会自动执行数据库引导（创建分库、账号、核心表、测试数据）并在最后做健康验收（含 A/B 登录冒烟）
- 默认种子数据（可重复初始化）包含：`11` 个测试账号、`7` 条活动、`13` 条报名记录、`4` 条公告、`3` 条反馈工单
- 四个服务库都内置 `event_outbox` 与 `mq_consume_record`，用于各服务独立的可靠投递与消费幂等
- `feedback.created` 事件在 `user-service` 会落地本地投影表 `user_feedback_projection`，不再只是记录消费日志
- Grafana 默认账号密码：`admin / admin`

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

本地 SQL 全量脚本与 Docker 一键部署都内置测试账号，密码均为 `password123`：

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
- [可观测栈说明](docs/observability/OBSERVABILITY.md)
- [目录结构](docs/overview/DIRECTORY_STRUCTURE.md)
- [技术说明](docs/overview/TECHNOLOGY_STACK.md)
- [项目交付摘要](docs/overview/PROJECT_SUMMARY.md)
- [验收清单](docs/setup/CHECKLIST.md)
- [前端工程说明](frontend2/README.md)
- [前端页面清单](docs/ui-page-inventory.md)
- [Stitch 页面规格](docs/stitch-page-spec.md)

## 8. 推荐阅读顺序

1. `README.md`
2. [docs/setup/QUICKSTART.md](docs/setup/QUICKSTART.md)
3. [docs/deploy/DEPLOY.md](docs/deploy/DEPLOY.md)
4. [docs/testing/API_TEST.md](docs/testing/API_TEST.md)
5. [docs/mcp/MCP_CONNECTION.md](docs/mcp/MCP_CONNECTION.md)
