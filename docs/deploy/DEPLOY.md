# 部署说明

本项目当前只保留三种部署形态：

1. 本机部署：`deploy/local`
2. Docker 单栈部署：根目录 `docker-compose.yml`
3. Kubernetes 部署：`deploy/k8s`

旧的多栈 Compose、蓝绿分流和多份 Compose 拆分文件已经移除。

## 1. 本机部署

适合开发调试。基础组件和应用服务都在本机运行，Nginx 只做静态资源托管和反向代理。

配置位置：

- `deploy/local/README.md`
- `deploy/local/nginx/cloud-demo.local.conf`

基本步骤：

```bash
mysql -u root -p < deploy/common/bootstrap-db.sql
mvn clean install -DskipTests
cd frontend2 && npm install && npm run build
```

后端建议启动顺序：

1. `UserApplication`
2. `ActivityApplication`
3. `AnnouncementApplication`
4. `FeedbackApplication`
5. `GatewayApplication`
6. `MonitorApplication`
7. `McpApplication`

默认访问：

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`
- 前端开发模式：`http://localhost:3000`

## 2. Docker 单栈部署

适合本机或单机服务器快速运行整套系统。每个服务只保留一个容器实例，不做蓝绿分流，也不做自动扩缩容。

配置位置：

- `docker-compose.yml`
- `deploy/docker/docker-compose.yml`
- `deploy/docker/.env.example`
- `deploy/docker/edge-nginx/`
- `deploy/docker/observability/`

启动：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

```bash
cp .env.example .env
docker compose up -d --build
```

关闭：

```powershell
docker compose down
```

```bash
docker compose down
```

默认访问：

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`
- Nacos：`http://localhost:8848/nacos`
- MinIO Console：`http://localhost:9006`

Docker 单栈流量路径：

```text
Browser -> edge-nginx -> gateway-service -> user/activity/announcement/feedback
```

## 3. Kubernetes 部署

适合演示更接近生产的部署拓扑。业务服务使用 Deployment，多副本通过 Kubernetes Service 进行负载均衡；中间件使用 StatefulSet 和 PVC。

配置位置：

- `deploy/k8s/README.md`
- `deploy/k8s/cloud-demo/*.yaml`
- `deploy/k8s/observability/*.yaml`
- `deploy/k8s/scripts/*`

部署：

```powershell
powershell -ExecutionPolicy Bypass -File deploy\k8s\scripts\apply-all.ps1
powershell -ExecutionPolicy Bypass -File deploy\k8s\scripts\init-db.ps1
```

```bash
bash deploy/k8s/scripts/apply-all.sh
bash deploy/k8s/scripts/init-db.sh
```

本机 Docker Desktop 访问方式：

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

清理：

```powershell
powershell -ExecutionPolicy Bypass -File deploy\k8s\scripts\delete-all.ps1
```

```bash
bash deploy/k8s/scripts/delete-all.sh
```

## 4. 通用数据库初始化

本机、Docker 和 Kubernetes 共用：

- `deploy/common/bootstrap-db.sql`

## 5. 当前不再保留的部署形态

以下旧形态已清理：

- 多栈 Compose
- 旧环境文件
- 根目录 Compose 拆分文件
- edge-nginx 50/50 分流配置
- 旧的启动、关闭、健康检查脚本
