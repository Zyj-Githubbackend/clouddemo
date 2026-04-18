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
- Docker MySQL 已经初始化过时，更新 `database/init.sql` 不会自动补表；新增公告功能可执行 `database/migrations/20260411_add_announcement.sql`，新增意见反馈功能可执行 `database/migrations/20260411_add_feedback.sql`

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
3. `AnnouncementApplication`
4. `FeedbackApplication`
5. `GatewayApplication`
6. `MonitorApplication`
7. `McpApplication`

本机默认端口：

- `8100`：`user-service`
- `8200`：`activity-service`
- `8300`：`announcement-service`
- `8400`：`feedback-service`
- `9000`：`gateway-service`
- `9100`：`monitor-service`
- `9300`：`mcp-service`

说明：

- `activity-service` 已内置 MinIO 默认值
- `announcement-service` 已内置 MinIO 默认值
- `feedback-service` 已内置 MinIO 默认值
- `mcp-service` 默认回调 `http://127.0.0.1:9000`
- 如需覆盖，可通过环境变量修改 `MINIO_*`、`CLOUD_DEMO_API_BASE_URL` 等配置

## 6. 启动前端

### 开发模式

```bash
cd frontend2
npm install
npm run dev
```

访问：`http://localhost:3000`

### 构建模式

```bash
cd frontend2
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
curl http://127.0.0.1:8400/actuator/health
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

## 11. Docker 快速启动（A/B 双栈）

如果希望整套环境通过 Docker 运行，当前推荐使用分层 compose：

```powershell
Copy-Item .env.example .env
# 编辑 .env，把 DEEPSEEK_API_KEY 填成本机 DeepSeek 密钥；不需要 AI 文案时可保持为空
# 如果部署机器无法稳定访问默认镜像源，可在 .env 里覆盖 *_IMAGE 变量为内网镜像地址
```

```bash
bash deploy/deploy-all.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/deploy-all.ps1
```

如果希望按阶段启动，也可以分别执行：

- `deploy/up-shared.*`
- `deploy/up-stack-a.*`
- `deploy/up-stack-b.*`
- `deploy/up-edge.*`

说明：当前推荐部署入口为 `compose.shared.yml`、`compose.stack.yml`、`compose.edge.yml`。根目录 `docker-compose.yml` 为历史单架构兼容文件，不再作为主入口。

一键部署会自动完成：

- shared MySQL 的分库与服务账号初始化
- 核心业务表初始化
- 开箱可测的示例数据写入（用户、活动、报名、公告、反馈）
- 部署完成后的健康验收（含 A/B 登录冒烟）

默认会写入的测试数据规模：

- 账号：11（`admin` + `student01` ~ `student10`）
- 活动：7（覆盖招募中、进行中、已完成、已取消）
- 报名记录：13
- 公告：4
- 反馈工单：3

访问地址：

- 前台：`http://localhost:8081/`
- 监控后台 A：`http://localhost:8081/monitor/a/`
- 监控后台 B：`http://localhost:8081/monitor/b/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

运行期日志：

- shared：`log/shared/`
- stack-a：`log/a/`
- stack-b：`log/b/`
- edge：`log/edge/`

说明：

- Docker A/B 模式默认只暴露 `edge-nginx` 的 `8081`，内部服务端口不直接映射到宿主机。
- Grafana 默认账号密码：`admin / admin`
- Prometheus 已按容器实例发现副本，Loki 也已支持按实例查询日志

如果 Docker 页面中文异常，优先执行：

```bash
bash deploy/down-all.sh
bash deploy/deploy-all.sh
```
