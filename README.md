# 校园志愿服务管理平台

基于 Spring Boot 3、Spring Cloud Alibaba、Vue 3 和 Nginx 的校园志愿服务管理平台。

## 项目概览

- 前端：Vue 3 + Vite + Element Plus
- 网关：`gateway-service`，端口 `9000`
- 用户服务：`user-service`，端口 `8100`
- 活动服务：`activity-service`，端口 `8200`
- 监控服务：`monitor-service`，端口 `9100`
- 注册中心：Nacos，端口 `8848`
- 数据库：MySQL，库名 `volunteer_platform`
- 缓存：Redis

## 近期更新

- 活动支持上传多张图片，后台创建和编辑活动时可维护图片列表，详情页可轮播展示
- 用户可在“我的志愿足迹”页面导出本人已核销志愿时长及对应活动明细 Excel
- Docker 版前端默认访问地址已调整为 `http://localhost:8081/`
- 本机 Jenkins 任务可在构建完成后直接调用 `docker compose up -d --build` 完成 Docker 发布

当前仓库已经补充了本机 Nginx 配置，推荐在本机通过 Nginx 统一访问：

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- 开发模式前端：`http://localhost:3000`

## 当前 Nginx 约定

仓库中的本机配置文件位于 [deploy/nginx/cloud-demo.local.conf](deploy/nginx/cloud-demo.local.conf)。

它约定了以下转发规则：

- `/`：前端静态文件 `frontend/dist`
- `/api/`：转发到 `http://127.0.0.1:9000/`
- `/monitor/`：转发到 `http://127.0.0.1:9100/`

为兼容 `/monitor/` 反向代理，`monitor-service` 已增加：

- `server.address=0.0.0.0`
- `server.forward-headers-strategy=framework`

## 本机启动摘要

1. 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

2. 启动 Redis 和 Nacos

3. 编译后端

```bash
mvn clean install -DskipTests
```

4. 启动后端服务

- `UserApplication`
- `ActivityApplication`
- `GatewayApplication`
- `MonitorApplication`

活动图片上传默认已经内置了这组 MinIO 配置，所以你本机直接启动 `ActivityApplication` 就能连到当前 MinIO：

```powershell
$env:MINIO_ENDPOINT="http://127.0.0.1:9005"
$env:MINIO_ACCESS_KEY="root"
$env:MINIO_SECRET_KEY="12345678"
$env:MINIO_BUCKET="activity-images"
```

如果你不设置环境变量，`activity-service` 会默认使用上面这组值。

如果数据库已经初始化过，请按实际情况执行以下其一：

1. 旧库还没有 `image_key` 字段：

```sql
ALTER TABLE vol_activity ADD COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

2. 字段已存在，但还是旧的 `VARCHAR(255)`：

```sql
ALTER TABLE vol_activity
MODIFY COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

5. 前端二选一

- 开发模式：`cd frontend && npm install && npm run dev`
- Nginx 模式：`cd frontend && npm run build`，然后启动 Nginx

## Docker 部署

仓库已经补充了以下 Docker 文件：

- [docker-compose.yml](docker-compose.yml)
- [services/user-service/Dockerfile](services/user-service/Dockerfile)
- [services/activity-service/Dockerfile](services/activity-service/Dockerfile)
- [services/gateway-service/Dockerfile](services/gateway-service/Dockerfile)
- [services/monitor-service/Dockerfile](services/monitor-service/Dockerfile)
- [frontend/Dockerfile](frontend/Dockerfile)
- [frontend/nginx.docker.conf](frontend/nginx.docker.conf)

启动命令：

```bash
docker compose up --build -d
```

默认访问地址：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- 网关直连：`http://localhost:9001`
- 监控直连：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO 控制台：`http://localhost:9008`

同校园网访问：

- 前台：`http://你的校园网IPv4:8081/`
- 监控后台：`http://你的校园网IPv4:8081/monitor/`
- 如果需要直连网关：`http://你的校园网IPv4:9001`

说明：

- Docker 版前端默认映射到 `8081`，避免和你本机已安装的 Nginx `80` 端口冲突
- Docker 版同时将 Nacos、网关、监控映射到 `8849`、`9001`、`9101`，避免和本机进程常用端口冲突
- Docker 版已内置 MinIO 容器，宿主机映射为 `9007` 和 `9008`
- 如果你希望 Docker 前端直接占用 `80`，可以把 `docker-compose.yml` 里的 `8081:80` 改成 `80:80`
- `database/init.sql` 顶部已显式设置 `SET NAMES utf8mb4;`，用于避免 Docker 初始化 MySQL 时中文按错误字符集导入
- Docker 模式下 `activity-service` 默认连接 Compose 内部地址 `http://minio:9000`
- MinIO 默认账号密码为 `root / 12345678`
- 若你改了端口或凭证，再在启动前覆盖 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`
- 如果你想把覆盖值长期保存，建议在仓库根目录参考 `.env.example` 新建 `.env`
- 同校园网访问前，先用 `ipconfig` 确认当前校园网 IPv4 地址
- 还需要放行 Windows 防火墙的 `8081` 端口；如果要让别人直连接口，再放行 `9001`
- 如果本机能打开 `http://localhost:8081/`，但同学打不开 `http://你的校园网IPv4:8081/`，通常是防火墙或校园网终端隔离导致

如果你在本机使用 Jenkins 触发 Docker 发布，构建成功后可直接执行：

```bat
cd /d D:\clouddemo\cloud-demo
docker compose up -d --build
```

如果你希望每次发版前先完整停掉旧容器，也可以改为：

```bat
cd /d D:\clouddemo\cloud-demo
docker compose down
docker compose up -d --build
```

如果你想单独构建某个微服务镜像，也可以直接使用它自己的 Dockerfile，例如：

```bash
docker build -f services/activity-service/Dockerfile -t cloud-demo/activity-service:latest .
docker build -f services/user-service/Dockerfile -t cloud-demo/user-service:latest .
docker build -f services/gateway-service/Dockerfile -t cloud-demo/gateway-service:latest .
docker build -f services/monitor-service/Dockerfile -t cloud-demo/monitor-service:latest .
```

如果你曾在旧配置下启动过 Docker，数据库卷里可能已经保留了乱码数据。此时需要重建数据卷：

```bash
docker compose down -v
docker compose up --build -d
```

## 默认测试账号

`database/init.sql` 内置了以下账号，密码均为 `password123`：

| 角色 | 用户名 | 说明 |
|------|------|------|
| 管理员 | `admin` | 可发布活动、签到、核销时长、查看统计 |
| 志愿者 | `student01` - `student10` | 用于普通用户流程测试 |

## 文档导航

- [快速开始](QUICKSTART.md)
- [部署说明](DEPLOY.md)
- [架构说明](ARCHITECTURE.md)
- [API 测试](API_TEST.md)
- [目录结构](DIRECTORY_STRUCTURE.md)
- [交付摘要](PROJECT_SUMMARY.md)
- [验收清单](CHECKLIST.md)

## 目录提示

- 后端代码：`services/`
- 前端代码：`frontend/`
- 数据库脚本：`database/init.sql`
- Nginx 配置：`deploy/nginx/`
