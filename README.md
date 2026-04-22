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
- 数据库：MySQL
- 缓存：Redis
- 图片存储：MinIO

## 2. 当前部署形态

本项目现在只保留三种部署方式：

| 方式 | 目录 | 说明 |
| --- | --- | --- |
| 本机部署 | `deploy/local` | 本机运行 MySQL、Redis、Nacos、MinIO、后端服务和前端，Nginx 做统一入口 |
| Docker 单栈 | `docker-compose.yml` / `deploy/docker` | 默认部署方式，每个服务一个容器，适合单机快速运行 |
| Kubernetes | `deploy/k8s` | 业务服务多副本 Deployment，中间件 StatefulSet + PVC，Ingress Controller 统一入口 |

旧的多栈 Compose、蓝绿分流和 Compose 拆分文件已经移除；根目录仅保留默认单机入口 `docker-compose.yml`。

## 3. 仓库结构

- `services/`：后端 Maven 聚合工程
- `frontend2/`：Vue 3 前端工程
- `deploy/local/`：本机部署配置
- `docker-compose.yml`：默认 Docker 单栈入口
- `deploy/docker/`：Docker 单栈部署配置
- `deploy/k8s/`：Kubernetes 部署配置
- `deploy/common/`：部署共用资源，例如 `bootstrap-db.sql`
- `docs/`：分主题文档中心

## 4. 本机运行摘要

```bash
mysql -u root -p < deploy/common/bootstrap-db.sql
mvn clean install -DskipTests
cd frontend2
npm install
npm run dev
```

生产静态资源模式可使用：

- `deploy/local/nginx/cloud-demo.local.conf`

访问：

- 前端开发模式：`http://localhost:3000`
- 本机 Nginx 模式：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`

## 5. Docker 单栈运行摘要

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

```bash
cp .env.example .env
docker compose up -d --build
```

访问：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`
- Nacos：`http://localhost:8848/nacos`
- MinIO Console：`http://localhost:9006`

关闭：

```powershell
docker compose down
```

## 6. Kubernetes 运行摘要

```powershell
powershell -ExecutionPolicy Bypass -File deploy\k8s\scripts\apply-all.ps1
powershell -ExecutionPolicy Bypass -File deploy\k8s\scripts\init-db.ps1
```

本机 Docker Desktop 访问：

```powershell
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 18081:80
```

hosts 增加：

```text
127.0.0.1 cloud-demo.local grafana.cloud-demo.local prometheus.cloud-demo.local
```

访问：

- `http://cloud-demo.local:18081/`
- `http://grafana.cloud-demo.local:18081/`
- `http://prometheus.cloud-demo.local:18081/`

## 7. 默认测试账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `password123` |
| 志愿者 | `student01` - `student10` | `password123` |

## 8. 文档导航

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
