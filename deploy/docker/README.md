# Docker 单栈部署

本目录只保留一套简单 Docker 部署：每个服务一个容器，不包含蓝绿分流或多副本扩容。根目录 `docker-compose.yml` 是默认入口，会引用本目录的 Compose 文件。

## 启动

可选：复制并修改环境变量。

```powershell
Copy-Item .env.example .env
```

启动：

```powershell
docker compose up -d --build
```

```bash
docker compose up -d --build
```

## 访问

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`，默认账号 `admin / admin`
- Prometheus：`http://localhost:9090`
- Nacos：`http://localhost:8848/nacos`
- MinIO Console：`http://localhost:9006`

## 关闭

```powershell
docker compose down
```

```bash
docker compose down
```

如需同时删除数据卷，可手动执行：

```bash
docker compose down -v
```
