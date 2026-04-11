# 快速开始

本文档给出一条最短可用路径：在本机启动全部核心服务，并通过 Nginx 统一访问前台、监控后台和 MCP。

## 1. 前置条件

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis
- Nacos 2.x
- Nginx
- MinIO

## 2. 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

注意：

- `database/init.sql` 会重建 `volunteer_platform`
- 请勿直接在已有正式数据的库上执行
- 脚本会导入默认账号、20 条活动及报名记录
- Docker MySQL 已经初始化过时，更新 `database/init.sql` 不会自动补表；新增公告功能可执行 `database/migrations/20260411_add_announcement.sql`

## 3. 启动 Redis、Nacos、MinIO

### Redis

```bash
redis-server
```

### Nacos

```bash
cd nacos/bin
startup.cmd -m standalone
```

启动后访问：`http://localhost:8848/nacos`

### MinIO

本机默认配置：

- API：`http://127.0.0.1:9005`
- Console：`http://127.0.0.1:9006`
- Access Key：`root`
- Secret Key：`12345678`
- Bucket：`activity-images`

示例启动命令：

```powershell
$env:MINIO_ROOT_USER="root"
$env:MINIO_ROOT_PASSWORD="12345678"
.\minio.exe server D:\miniodata\data2 --console-address "127.0.0.1:9006" --address "127.0.0.1:9005"
```

如果你使用的是旧数据库，需要确认 `vol_activity.image_key` 已升级为 `TEXT`：

```sql
ALTER TABLE vol_activity ADD COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

或者：

```sql
ALTER TABLE vol_activity
MODIFY COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

## 4. 编译后端

```bash
mvn clean install -DskipTests
```

## 5. 启动后端服务

建议顺序：

1. `UserApplication`
2. `ActivityApplication`
3. `GatewayApplication`
4. `MonitorApplication`
5. `McpApplication`

本机默认端口：

- `8100`：`user-service`
- `8200`：`activity-service`
- `8300`：`announcement-service`
- `9000`：`gateway-service`
- `9100`：`monitor-service`
- `9300`：`mcp-service`

说明：

- `activity-service` 已内置 MinIO 默认值
- `announcement-service` 已内置 MinIO 默认值
- `mcp-service` 默认回调 `http://127.0.0.1:9000`
- 如需覆盖，可通过环境变量修改 `MINIO_*`、`CLOUD_DEMO_API_BASE_URL` 等配置

## 6. 启动前端

### 开发模式

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:3000`

### 构建模式

```bash
cd frontend
npm install
npm run build
```

构建完成后使用仓库内的 Nginx 配置：

- [deploy/nginx/cloud-demo.local.conf](../../deploy/nginx/cloud-demo.local.conf)

## 7. 验证入口

### 本机 Nginx 模式

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`

### 直连验证

- 网关：`http://localhost:9000`
- 监控：`http://localhost:9100`
- Nacos：`http://localhost:8848/nacos`
- MinIO Console：`http://127.0.0.1:9006`

## 8. 默认测试账号

- 管理员：`admin / password123`
- 志愿者：`student01 / password123`

## 9. 快速验证命令

```bash
curl http://127.0.0.1/
curl "http://127.0.0.1:9000/activity/list?page=1&size=10"
curl "http://127.0.0.1:9000/announcement/home?limit=5"
curl http://127.0.0.1:9100/actuator/health
curl http://127.0.0.1:9300/actuator/health
```

登录验证：

```bash
curl -X POST http://127.0.0.1:9000/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

## 10. 常见问题

### 登录返回 503

- 检查 `user-service` 是否已注册到 Nacos
- 检查 `gateway-service` 是否正常启动

### 页面能开但接口报错

- 检查 `/api/` 是否正确代理到 `9000`
- 检查前端 `baseURL` 是否仍为 `/api`

### `/monitor/` 打不开

- 检查 `monitor-service` 是否运行在 `9100`
- 检查 Nginx 是否带有 `X-Forwarded-Prefix /monitor`

### MCP OAuth 元数据不正确

- 检查 `/.well-known/oauth-authorization-server`
- 检查 Nginx 是否已加载最新 `/authorize`、`/token`、`/register` 与 `/mcp` 代理规则

## 11. Docker 快速启动

如果希望整套环境通过 Docker 运行：

```powershell
Copy-Item .env.example .env
# 编辑 .env，把 DEEPSEEK_API_KEY 填成本机 DeepSeek 密钥；不需要 AI 文案时可保持为空
```

```bash
docker compose up -d --build
```

说明：`docker-compose.yml` 会从宿主机环境变量或仓库根目录 `.env` 读取 `DEEPSEEK_API_KEY`，再传给容器内的 `activity-service`。

访问地址：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- 网关：`http://localhost:9001`
- 监控：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO Console：`http://localhost:9008`

如果 Docker 页面中文异常，优先执行：

```bash
docker compose down -v
docker compose up -d --build
```
