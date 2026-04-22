# 快速开始

本文档给出两条最短可用路径：本机运行和 Docker 单栈运行。Kubernetes 详见 [部署说明](../deploy/DEPLOY.md) 与 [deploy/k8s/README.md](../../deploy/k8s/README.md)。

## 1. 本机运行

前置条件：

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis
- Nacos 2.x
- MinIO
- Nginx

初始化数据库：

```bash
mysql -u root -p < deploy/common/bootstrap-db.sql
```

启动基础组件：

- Redis：`127.0.0.1:6379`
- Nacos：`127.0.0.1:8848`
- MinIO API：`127.0.0.1:9005`
- MinIO Console：`127.0.0.1:9006`

编译后端：

```bash
mvn clean install -DskipTests
```

建议启动顺序：

1. `UserApplication`
2. `ActivityApplication`
3. `AnnouncementApplication`
4. `FeedbackApplication`
5. `GatewayApplication`
6. `MonitorApplication`
7. `McpApplication`

启动前端：

```bash
cd frontend2
npm install
npm run dev
```

开发模式访问：

- 前端：`http://localhost:3000`
- 网关：`http://localhost:9000`
- 监控：`http://localhost:9100`
- MCP：`http://localhost:9300/mcp`

生产静态资源模式：

```bash
cd frontend2
npm install
npm run build
```

使用 Nginx 配置：

- [deploy/local/nginx/cloud-demo.local.conf](../../deploy/local/nginx/cloud-demo.local.conf)

访问：

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`

## 2. Docker 单栈运行

Docker 部署现在只有一套服务，每个服务一个容器。

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

```bash
docker compose down
```

## 3. 默认测试账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `password123` |
| 志愿者 | `student01` - `student10` | `password123` |

## 4. 快速验证

```bash
curl http://127.0.0.1:9000/actuator/health
curl http://127.0.0.1:9100/actuator/health
curl http://127.0.0.1:9300/actuator/health
```

登录验证：

```bash
curl -X POST http://127.0.0.1:9000/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

Docker 入口验证：

```bash
curl http://127.0.0.1:8081/
curl http://127.0.0.1:8081/api/user/health
```
