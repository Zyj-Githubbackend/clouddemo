# 部署说明

本文档聚焦当前仓库已经验证过的三种部署方式：本机 Windows + Nginx、Docker Compose，以及本机 Jenkins 调用 Docker Compose 自动发布。冗长的 CI/CD 和大段示例已移除，避免与实际仓库状态脱节。

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

如果数据库不是刚用 `database/init.sql` 初始化，而是已有存量库，请按实际情况执行以下其一：

```sql
ALTER TABLE vol_activity ADD COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

或者：

```sql
ALTER TABLE vol_activity
MODIFY COLUMN image_key TEXT COMMENT '活动图片对象键列表，逗号分隔';
```

其中：

- `ADD COLUMN` 适用于老库没有该字段
- `MODIFY COLUMN` 适用于字段已存在，但长度仍是旧版单图场景的 `VARCHAR(255)`

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

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- 网关：`http://localhost:9001`
- 监控：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`
- MinIO API：`http://localhost:9007`
- MinIO 控制台：`http://localhost:9008`

同校园网访问地址：

- 前台：`http://你的校园网IPv4:8081/`
- 监控后台：`http://你的校园网IPv4:8081/monitor/`
- 网关：`http://你的校园网IPv4:9001`

说明：

- 前端容器端口使用 `8081:80`，避免和宿主机现有 Nginx 冲突
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
- Windows 防火墙至少需要放行 `8081`；如果需要让同学直接调接口，再额外放行 `9001`
- 如果同学无法访问，而你本机可以访问 `http://localhost:8081/`，优先排查防火墙和校园网是否启用了终端互访隔离

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

## 三、本机 Jenkins 自动发布

如果 Jenkins 就安装在当前这台 Windows 机器上，最简单的方式是让 Jenkins 在现有构建步骤之后，先执行一次 Maven 测试，再执行 Docker Compose。

Jenkins 自由风格项目推荐使用：

- `Build` -> `Execute Windows batch command`

命令示例：

```bat
cd /d D:\clouddemo\cloud-demo
mvn -B test
docker compose up -d --build
docker compose ps
```

如果你希望每次发布前先完整停掉旧容器，可以使用：

```bat
cd /d D:\clouddemo\cloud-demo
mvn -B test
docker compose down
docker compose up -d --build
docker compose ps
```

说明：

- 这种方式适合 Jenkins 与 Docker Desktop 都在本机的场景
- `mvn -B test` 会执行当前仓库已经接入的 JUnit 5 测试，测试失败时 Jenkins 应停止后续部署
- `up -d --build` 会保留已有数据卷，不会自动修改数据库表结构
- 日常发布一般不建议直接使用 `docker compose down -v`，除非你明确要清空 MySQL 和 MinIO 数据
- 如果 Jenkins 控制台里 Docker 已执行成功，但后续还有“自动创建 PR”之类的步骤报错，任务仍可能被 Jenkins 标红，此时需要单独调整 Jenkins 后续步骤

测试报告建议：

- Jenkins 构建后操作可使用 `Publish JUnit test result report`
- 报告路径填写 `**/target/surefire-reports/*.xml`

## 四、监控后台说明

`monitor-service` 当前已适配反向代理：

- `server.address=0.0.0.0`
- `server.forward-headers-strategy=framework`

因此推荐通过 Nginx 访问：

- `http://localhost/monitor/`

而不是直接暴露 `http://localhost:9100/` 作为最终入口。

## 五、Linux 服务器部署摘要

如果后续迁移到 Linux，可以沿用同一思路：

1. `npm run build`
2. 将 `frontend/dist` 发布到站点目录
3. 将 `/api/` 代理到 `9000`
4. 将 `/monitor/` 代理到 `9100`
5. 后端服务通过 `systemd` 或其他进程管理工具启动

## 六、排查命令

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
curl http://127.0.0.1:8081/
netstat -ano | findstr :8081
```

## 七、部署验收清单

- [ ] `database/init.sql` 已执行
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] `user-service`、`activity-service`、`gateway-service`、`monitor-service` 已启动
- [ ] `frontend/dist` 已生成
- [ ] Nginx 已成功加载配置
- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
- [ ] 或者 Docker 模式下 `http://localhost:8081/` 可访问
- [ ] 或者 Docker 模式下 `http://localhost:8081/monitor/` 可访问
- [ ] 如果需要同校园网访问，`http://你的校园网IPv4:8081/` 可从另一台设备打开
