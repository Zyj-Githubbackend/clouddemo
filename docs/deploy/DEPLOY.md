# 部署说明

本文档聚焦当前仓库已经对齐的三种方式：

1. 本机 Windows + Nginx
2. Docker Compose
3. 本机 Jenkins 调用 Docker Compose 自动发布

## 1. 目标拓扑

```text
Browser
  └─ Nginx :80
      ├─ /                -> frontend/dist
      ├─ /api/            -> gateway-service:9000
      ├─ /monitor/        -> monitor-service:9100
      ├─ /.well-known/*   -> mcp-service:9300
      ├─ /authorize       -> mcp-service:9300
      ├─ /token           -> mcp-service:9300
      ├─ /register        -> mcp-service:9300
      └─ /mcp*            -> mcp-service:9300
```

## 2. 服务端口

| 组件 | 本机端口 | Docker 宿主机端口 |
|------|------|------|
| MySQL | 3306 | 容器内部 |
| Redis | 6379 | 容器内部 |
| Nacos | 8848 | 8849 |
| user-service | 8100 | 容器内部 |
| activity-service | 8200 | 容器内部 |
| gateway-service | 9000 | 9001 |
| monitor-service | 9100 | 9101 |
| mcp-service | 9300 | 由前端 Nginx 统一代理 |
| 前端 Nginx | 80 | 8081 |
| MinIO API | 9005 | 9007 |
| MinIO Console | 9006 | 9008 |

## 3. 本机 Windows + Nginx

### 3.1 初始化依赖

```bash
mysql -u root -p < database/init.sql
redis-server
```

Nacos：

```bash
cd nacos/bin
startup.cmd -m standalone
```

MinIO 可使用本机默认参数启动，详见 [快速开始](../setup/QUICKSTART.md)。

### 3.2 编译并启动后端

```bash
mvn clean install -DskipTests
```

启动：

- `UserApplication`
- `ActivityApplication`
- `GatewayApplication`
- `MonitorApplication`
- `McpApplication`

### 3.3 构建前端

```bash
cd frontend
npm install
npm run build
```

### 3.4 配置 Nginx

仓库已提供本机配置片段：

- [deploy/nginx/cloud-demo.local.conf](../../deploy/nginx/cloud-demo.local.conf)

当前配置核心行为：

- `root` 指向 `D:/clouddemo/cloud-demo/frontend/dist`
- `/api/` 转发到 `127.0.0.1:9000`
- `/monitor/` 转发到 `127.0.0.1:9100`
- `/.well-known/*`、`/authorize`、`/token`、`/register`、`/mcp*` 转发到 `127.0.0.1:9300`

### 3.5 启动或重载 Nginx

```powershell
cd D:\nginx-1.28.3
.\nginx.exe -t
.\nginx.exe
```

重载：

```powershell
.\nginx.exe -s reload
```

### 3.6 本机验证

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`

## 4. Docker Compose

仓库已提供完整 Compose 方案，包含：

- MySQL
- Redis
- Nacos
- MinIO
- `user-service`
- `activity-service`
- `gateway-service`
- `monitor-service`
- `mcp-service`
- 前端 Nginx 容器

启动命令：

```bash
docker compose up -d --build
```

停止命令：

```bash
docker compose down
```

访问地址：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- 网关：`http://localhost:9001`
- 监控：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO Console：`http://localhost:9008`

### 4.1 环境变量覆盖

可在仓库根目录参考 `.env.example` 创建 `.env`：

- `DEEPSEEK_API_KEY`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `MINIO_PUBLIC_BASE_URL`
- `CLOUD_DEMO_API_BASE_URL`

Docker 模式下的默认约定：

- `activity-service` 默认连接 `http://minio:9000`
- `MINIO_PUBLIC_BASE_URL` 默认为 `/api`
- `mcp-service` 默认回调 `http://gateway-service:9000`

### 4.2 日志挂载

Compose 当前会将容器日志挂载到仓库根目录：

- `./user-service/logs`
- `./activity-service/logs`
- `./gateway-service/logs`
- `./monitor-service/logs`

这几处目录是运行产物目录，不是源码目录。

### 4.3 常见维护命令

重建代理相关服务：

```bash
docker compose up -d --build frontend monitor-service mcp-service
```

查看容器：

```bash
docker compose ps
```

彻底重建数据库卷：

```bash
docker compose down -v
docker compose up -d --build
```

### 4.4 Docker 中文乱码排查

如果 Docker 页面或接口中的中文已经出现乱码，通常是旧 MySQL 数据卷中保存了错误编码的数据。建议执行：

```bash
docker compose down -v
docker compose up -d --build
```

## 5. 单独构建镜像

如果不通过 Compose，也可以在仓库根目录单独构建：

```bash
docker build -f services/user-service/Dockerfile -t cloud-demo/user-service:latest .
docker build -f services/activity-service/Dockerfile -t cloud-demo/activity-service:latest .
docker build -f services/gateway-service/Dockerfile -t cloud-demo/gateway-service:latest .
docker build -f services/monitor-service/Dockerfile -t cloud-demo/monitor-service:latest .
docker build -f services/mcp-service/Dockerfile -t cloud-demo/mcp-service:latest .
docker build -f frontend/Dockerfile -t cloud-demo/frontend:latest .
```

## 6. Jenkins 自动发布

如果 Jenkins 与 Docker Desktop 都在本机 Windows 上，推荐在自由风格任务中使用：

```bat
cd /d D:\clouddemo\cloud-demo
mvn -B test
docker compose up -d --build
docker compose ps
```

如果你希望先停掉旧容器：

```bat
cd /d D:\clouddemo\cloud-demo
mvn -B test
docker compose down
docker compose up -d --build
docker compose ps
```

测试报告路径：

```text
**/target/surefire-reports/*.xml
```

## 7. 监控与 MCP 补充说明

### 监控后台

`monitor-service` 已启用：

- `server.address=0.0.0.0`
- `server.forward-headers-strategy=framework`

因此推荐通过：

- `http://localhost/monitor/`
- `http://localhost:8081/monitor/`

访问监控页面。

### MCP

`mcp-service` 已启用：

- `server.forward-headers-strategy=framework`
- Spring Boot Admin Client 自动注册
- Streamable HTTP MCP 端点 `/mcp`

OAuth 相关元数据与登录页面同样建议经过 Nginx 暴露。

## 8. 排查命令

```powershell
netstat -ano | findstr :80
netstat -ano | findstr :9000
netstat -ano | findstr :9100
netstat -ano | findstr :9300
curl http://127.0.0.1/
curl "http://127.0.0.1:9000/activity/list?page=1&size=10"
curl http://127.0.0.1:9100/actuator/health
curl http://127.0.0.1:9300/actuator/health
```
