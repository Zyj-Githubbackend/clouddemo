# 部署说明（A/B 双栈 + 共享基础组件 + Edge 统一入口）

本文档对应当前仓库的**最终部署骨架**：

- 共享组件：`mysql`、`redis`、`minio`、`mcp-service`
- 共享观测组件：`otel-collector`、`prometheus`、`grafana`、`loki`、`promtail`、`tempo`
- A 栈：`gateway-a`、`user-service-a`(2)、`activity-service-a`(2)、`announcement-service-a`(2)、`feedback-service-a`(2)、`nacos-a`、`rabbitmq-a`、`monitor-a`
- B 栈：`gateway-b`、`user-service-b`(2)、`activity-service-b`(2)、`announcement-service-b`(2)、`feedback-service-b`(2)、`nacos-b`、`rabbitmq-b`、`monitor-b`
- 边缘入口：`edge-nginx`

> 说明：`mysql` 当前仍为共享实例；前端静态资源由 `edge-nginx` 镜像构建阶段统一打包并托管，不再保留栈内 frontend 服务。

---

## 1. 架构图

```text
Browser
  -> edge-nginx (:8081)
      |- /                  -> frontend2/dist (SPA)
      |- /api/*             -> gateway-a 或 gateway-b (A/B 分流)
      |- /monitor/a/*       -> monitor-a
      |- /monitor/b/*       -> monitor-b
      |- /mcp* + OAuth 路径 -> mcp-service

shared:
  mysql, redis, minio, mcp-service

stack-a:
  nacos-a, rabbitmq-a, monitor-a, gateway-a,
  user-service-a(x2), activity-service-a(x2),
  announcement-service-a(x2), feedback-service-a(x2)

stack-b:
  nacos-b, rabbitmq-b, monitor-b, gateway-b,
  user-service-b(x2), activity-service-b(x2),
  announcement-service-b(x2), feedback-service-b(x2)
```

---

## 2. Compose 文件说明

- `compose.shared.yml`
  - 包含：`mysql`、`redis`、`minio`、`mcp-service` 与 shared 层可观测组件
- `compose.stack.yml`
  - 仅包含：`nacos`、`rabbitmq`、`monitor-service`、`gateway-service`、`user-service`、`activity-service`、`announcement-service`、`feedback-service`
  - 同一份文件通过 `--env-file` + `-p` 区分 A/B 栈
  - `user-service`、`activity-service`、`announcement-service`、`feedback-service` 的默认副本数由 `deploy/stack-a.env` / `deploy/stack-b.env` 中的 `*_SERVICE_REPLICAS` 控制，普通 `docker compose up -d --build` 不会再悄悄缩回 1
- `compose.edge.yml`
  - 仅包含：`edge-nginx`
- `docker-compose.yml`
  - 历史单架构 compose，保留作兼容参考，不是当前推荐入口

---

## 3. 日志目录与挂载

当前 Docker 运行期日志统一挂载到仓库根目录 `log/`：

- `log/shared/mcp-service/`：shared 层 MCP 日志
- `log/a/{gateway-service,user-service,activity-service,announcement-service,feedback-service,monitor-service}/`：A 栈日志
- `log/b/{gateway-service,user-service,activity-service,announcement-service,feedback-service,monitor-service}/`：B 栈日志
- `log/edge/edge-nginx/`：edge nginx 访问与错误日志

说明：

1. 业务服务容器内仍写入 `/app/logs/debug.log`
2. edge-nginx 写入 `/var/log/nginx/access.log` 与 `/var/log/nginx/error.log`
3. 宿主机排障时优先查看 `log/`，而不是进入源码目录寻找日志
4. Loki/Promtail 当前以 Docker 容器发现为主，`edge` 保留文件日志兜底

---

## 4. 可观测栈

shared 层当前包含：

- `prometheus`
- `grafana`
- `loki`
- `promtail`
- `tempo`
- `otel-collector`

宿主机访问地址：

- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

Grafana 默认账号：`admin / admin`

说明：

1. Prometheus 已改为按 Docker 容器实例抓取 `/actuator/prometheus`
2. Loki 已通过 Promtail 按容器实例采集业务服务日志
3. Grafana 已预置 Prometheus、Loki、Tempo 数据源
4. 详细使用方式见 [../observability/OBSERVABILITY.md](../observability/OBSERVABILITY.md)

---

## 5. 网络隔离

当前部署使用以下网络：

- `edge-a-net`：`edge-nginx <-> gateway-a、monitor-a`
- `edge-b-net`：`edge-nginx <-> gateway-b、monitor-b`
- `edge-shared-net`：`edge-nginx <-> mcp-service`
- `shared-a-net`：A 栈服务与 `mysql/redis/minio/mcp-service`
- `shared-b-net`：B 栈服务与 `mysql/redis/minio/mcp-service`

栈内还使用独立内部网络：

- `stack-a-internal-net`
- `stack-b-internal-net`

通过网络与服务命名双重隔离，A/B 默认互不可见。

---

## 6. 启动命令

### 6.1 shared

Linux/macOS:

```bash
bash deploy/up-shared.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/up-shared.ps1
```

### 6.2 stack-a

Linux/macOS:

```bash
bash deploy/up-stack-a.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/up-stack-a.ps1
```

### 6.3 stack-b

Linux/macOS:

```bash
bash deploy/up-stack-b.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/up-stack-b.ps1
```

### 6.4 edge

Linux/macOS:

```bash
bash deploy/up-edge.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/up-edge.ps1
```

---

## 7. 一键部署 / 一键关闭

一键部署（顺序：shared -> stack-a -> stack-b -> edge）：

```bash
bash deploy/deploy-all.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/deploy-all.ps1
```

一键关闭：

```bash
bash deploy/down-all.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/down-all.ps1
```

---

## 8. 扩容命令

当前默认副本数来自：

- `deploy/stack-a.env`
- `deploy/stack-b.env`

默认值：

- `USER_SERVICE_REPLICAS=2`
- `ACTIVITY_SERVICE_REPLICAS=2`
- `ANNOUNCEMENT_SERVICE_REPLICAS=2`
- `FEEDBACK_SERVICE_REPLICAS=2`

对 A 栈扩容示例：先修改 `deploy/stack-a.env`：

```env
USER_SERVICE_REPLICAS=3
ACTIVITY_SERVICE_REPLICAS=3
ANNOUNCEMENT_SERVICE_REPLICAS=3
FEEDBACK_SERVICE_REPLICAS=3
```

然后执行：

```bash
bash deploy/up-stack-a.sh
```

对 B 栈扩容示例：先修改 `deploy/stack-b.env` 后再执行：

```bash
bash deploy/up-stack-b.sh
```

---

## 9. 验证命令

容器状态：

```bash
docker compose -p shared -f compose.shared.yml ps
docker compose -p stack-a --env-file deploy/stack-a.env -f compose.stack.yml ps
docker compose -p stack-b --env-file deploy/stack-b.env -f compose.stack.yml ps
docker compose -p edge -f compose.edge.yml ps
```

访问验证：

- 前台：`http://localhost:8081/`
- 监控 A：`http://localhost:8081/monitor/a/`
- 监控 B：`http://localhost:8081/monitor/b/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

说明：当前 Docker A/B 模式默认只暴露 `edge-nginx` 的 `8081`，内部网关、监控、Nacos、MinIO 等端口不再直接映射到宿主机。

网关 A/B 连通性（容器内）：

```bash
docker compose -p edge -f compose.edge.yml exec edge-nginx sh -lc "wget -qO- http://gateway-a:9000/actuator/health"
docker compose -p edge -f compose.edge.yml exec edge-nginx sh -lc "wget -qO- http://gateway-b:9000/actuator/health"
```

数据库迁移（旧数据卷场景）：

```bash
docker compose -p shared -f compose.shared.yml exec -T mysql \
  mysql --default-character-set=utf8mb4 -uroot -p123888 volunteer_platform \
  < database/migrations/20260415_add_messaging_outbox.sql
```

---

## 10. 排障命令

查看日志：

```bash
docker compose -p stack-a --env-file deploy/stack-a.env -f compose.stack.yml logs -f gateway-service
docker compose -p stack-b --env-file deploy/stack-b.env -f compose.stack.yml logs -f gateway-service
docker compose -p shared -f compose.shared.yml logs -f mcp-service
docker compose -p edge -f compose.edge.yml logs -f edge-nginx
```

查看宿主机落盘日志：

```bash
ls log/shared/mcp-service
ls log/a/gateway-service
ls log/b/gateway-service
ls log/edge/edge-nginx
```

查看 Prometheus targets：

```bash
curl http://127.0.0.1:9090/api/v1/targets
```

查看 Loki 实例标签：

```bash
docker run --rm --network shared-a-net curlimages/curl:8.10.1 -sS "http://loki:3100/loki/api/v1/label/instance/values"
```

查看网络：

```bash
docker network ls | grep -E "edge-|shared-|stack-"
```

检查 outbox / 幂等记录：

```bash
docker compose -p shared -f compose.shared.yml exec mysql \
  mysql -uroot -p123888 -e "SELECT status, COUNT(*) c FROM volunteer_platform.event_outbox GROUP BY status;"

docker compose -p shared -f compose.shared.yml exec mysql \
  mysql -uroot -p123888 -e "SELECT consumer_name, COUNT(*) c FROM volunteer_platform.mq_consume_record GROUP BY consumer_name;"
```

---

## 11. 关键约束回顾

- 同步调用：OpenFeign + Spring Cloud LoadBalancer（服务直连，不绕 gateway）
- 异步解耦：RabbitMQ（不把所有调用都改成 MQ）
- A 栈仅连接 `nacos-a` / `rabbitmq-a`
- B 栈仅连接 `nacos-b` / `rabbitmq-b`
- 可扩副本服务不设置 `container_name`
