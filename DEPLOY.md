# 部署说明

本文档聚焦当前仓库已经验证过的两种部署方式：本机 Windows + Nginx，以及 Docker Compose。冗长的 CI/CD 和大段示例已移除，避免与实际仓库状态脱节。

## 目标拓扑

```text
浏览器
  └─ Nginx :80
      ├─ /            -> frontend/dist
      ├─ /api/        -> gateway-service:9000
      └─ /monitor/    -> monitor-service:9100
```

## 服务端口

| 组件 | 端口 |
|------|------|
| MySQL | 3306 |
| Redis | 6379 |
| Nacos | 8848 |
| user-service | 8100 |
| activity-service | 8200 |
| gateway-service | 9000 |
| monitor-service | 9100 |
| Nginx | 80 |

## 一、本机 Windows 部署

### 1. 初始化基础设施

```bash
mysql -u root -p < database/init.sql
redis-server
```

Nacos：

```bash
cd nacos/bin
startup.cmd -m standalone
```

### 2. 编译并启动后端

```bash
mvn clean install -DskipTests
```

活动图片上传默认已经内置了这组 MinIO 配置，本机直接启动 `ActivityApplication` 即可：

```powershell
$env:MINIO_ENDPOINT="http://127.0.0.1:9005"
$env:MINIO_ACCESS_KEY="root"
$env:MINIO_SECRET_KEY="12345678"
$env:MINIO_BUCKET="activity-images"
```

如果你不设置环境变量，`activity-service` 会默认使用上面这组值。

如果数据库不是刚用 `database/init.sql` 初始化，而是已有存量库，请先执行：

```sql
ALTER TABLE vol_activity ADD COLUMN image_key VARCHAR(255) COMMENT '活动图片对象键';
```

启动：

- `UserApplication`
- `ActivityApplication`
- `GatewayApplication`
- `MonitorApplication`

### 3. 构建前端

```bash
cd frontend
npm install
npm run build
```

构建产物目录为 `frontend/dist`。

### 4. 配置 Nginx

仓库已提供本机配置片段：

- [deploy/nginx/cloud-demo.local.conf](deploy/nginx/cloud-demo.local.conf)

如果你的 Windows Nginx 使用单文件 `conf/nginx.conf`，请将该片段合并到 `http {}` 内。

当前配置核心行为：

- `root` 指向 `D:/clouddemo/cloud-demo/frontend/dist`
- `/api/` 转发到 `127.0.0.1:9000`
- `/monitor/` 转发到 `127.0.0.1:9100`

### 5. 启动或重载 Nginx

示例：

```powershell
cd D:\nginx-1.28.3
.\nginx.exe -t
.\nginx.exe
```

修改配置后重载：

```powershell
.\nginx.exe -s reload
```

### 6. 访问验证

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`

如果希望同校园网同学访问，需要：

- 使用当前校园网 IPv4 地址访问，例如 `http://你的局域网IP/`
- 放行 Windows 防火墙的 `80` 端口

## 二、Docker Compose 部署

仓库已提供完整的 Compose 方案，包含：

- MySQL
- Redis
- Nacos
- MinIO
- `user-service`
- `activity-service`
- `gateway-service`
- `monitor-service`
- 前端 Nginx 容器

并且每个微服务现在都有独立 Dockerfile，可用于单独构建和发布：

- [services/user-service/Dockerfile](services/user-service/Dockerfile)
- [services/activity-service/Dockerfile](services/activity-service/Dockerfile)
- [services/gateway-service/Dockerfile](services/gateway-service/Dockerfile)
- [services/monitor-service/Dockerfile](services/monitor-service/Dockerfile)

启动命令：

```bash
docker compose up --build -d
```

停止命令：

```bash
docker compose down
```

默认访问地址：

- 前台：`http://localhost:8080/`
- 监控后台：`http://localhost:8080/monitor/`
- 网关：`http://localhost:9001`
- 监控：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO 控制台：`http://localhost:9008`

同校园网访问地址：

- 前台：`http://你的校园网IPv4:8080/`
- 监控后台：`http://你的校园网IPv4:8080/monitor/`
- 网关：`http://你的校园网IPv4:9001`

说明：

- 前端容器端口使用 `8080:80`，避免和宿主机现有 Nginx 冲突
- Nacos、网关、监控的宿主机端口分别为 `8849`、`9001`、`9101`，避免和本机同名服务冲突
- Compose 已内置 MinIO，宿主机端口映射为 `9007`（API）和 `9008`（控制台）
- 数据库初始化脚本会挂载 `database/init.sql`
- 如果宿主机已设置 `DEEPSEEK_API_KEY`，Compose 会透传给 `activity-service`
- Docker 内部默认使用 `http://minio:9000`
- Compose 默认使用 MinIO 账号 `root`、密码 `12345678`、bucket `activity-images`
- 如果 MinIO 的账号、密码或 bucket 与默认值不同，请在 `docker compose up` 前设置 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET`
- 如果你希望这些覆盖值持久保留，建议参考仓库根目录 `.env.example` 创建 `.env`
- `database/init.sql` 顶部已加入 `SET NAMES utf8mb4;`，用于避免 MySQL 容器初始化时中文按错误字符集导入
- 同校园网分享时，请先通过 `ipconfig` 确认宿主机当前 IPv4 地址
- Windows 防火墙至少需要放行 `8080`；如果需要让同学直接调接口，再额外放行 `9001`
- 如果同学无法访问，而你本机可以访问 `http://localhost:8080/`，优先排查防火墙和校园网是否启用了终端互访隔离

### 单独构建某个微服务

如果你不是整套用 Compose，而是要单独发布某个微服务，可以直接在仓库根目录执行：

```bash
docker build -f services/user-service/Dockerfile -t cloud-demo/user-service:latest .
docker build -f services/activity-service/Dockerfile -t cloud-demo/activity-service:latest .
docker build -f services/gateway-service/Dockerfile -t cloud-demo/gateway-service:latest .
docker build -f services/monitor-service/Dockerfile -t cloud-demo/monitor-service:latest .
```

随后按你的部署环境，为容器补充对应的数据库、Redis、Nacos、JWT、MinIO 等环境变量即可。

### Docker 中文乱码修复

如果 Docker 部署后页面或接口中的中文已经出现乱码，通常不是前端渲染问题，而是旧的 MySQL 数据卷里已经写入了错误编码的数据。

修复步骤：

```bash
docker compose down -v
docker compose up --build -d
```

说明：

- `down -v` 会删除 Compose 创建的 `mysql-data` 卷
- 重新启动后会按当前的 `database/init.sql` 重新初始化数据库
- 如果卷不删除，旧乱码数据会继续保留

## 三、监控后台说明

`monitor-service` 当前已适配反向代理：

- `server.address=0.0.0.0`
- `server.forward-headers-strategy=framework`

因此推荐通过 Nginx 访问：

- `http://localhost/monitor/`

而不是直接暴露 `http://localhost:9100/` 作为最终入口。

## 四、Linux 服务器部署摘要

如果后续迁移到 Linux，可以沿用同一思路：

1. `npm run build`
2. 将 `frontend/dist` 发布到站点目录
3. 将 `/api/` 代理到 `9000`
4. 将 `/monitor/` 代理到 `9100`
5. 后端服务通过 `systemd` 或其他进程管理工具启动

## 五、排查命令

端口监听：

```powershell
netstat -ano | findstr :80
netstat -ano | findstr :9000
netstat -ano | findstr :9100
```

前端和代理联通性：

```powershell
curl http://127.0.0.1/
curl http://127.0.0.1:9000/activity/list?page=1&size=10
curl http://127.0.0.1:9100/
```

Docker 前端联通性：

```powershell
ipconfig
curl http://127.0.0.1:8080/
netstat -ano | findstr :8080
```

## 六、部署验收清单

- [ ] `database/init.sql` 已执行
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] `user-service`、`activity-service`、`gateway-service`、`monitor-service` 已启动
- [ ] `frontend/dist` 已生成
- [ ] Nginx 已成功加载配置
- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
- [ ] 或者 Docker 模式下 `http://localhost:8080/` 可访问
- [ ] 或者 Docker 模式下 `http://localhost:8080/monitor/` 可访问
- [ ] 如果需要同校园网访问，`http://你的校园网IPv4:8080/` 可从另一台设备打开
