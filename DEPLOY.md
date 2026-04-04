# 部署说明

本文档聚焦当前仓库已经验证过的部署方式：本机 Windows + Nginx。冗长的 Docker、CI/CD 和大段示例已移除，避免与实际仓库状态脱节。

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

## 二、监控后台说明

`monitor-service` 当前已适配反向代理：

- `server.address=0.0.0.0`
- `server.forward-headers-strategy=framework`

因此推荐通过 Nginx 访问：

- `http://localhost/monitor/`

而不是直接暴露 `http://localhost:9100/` 作为最终入口。

## 三、Linux 服务器部署摘要

如果后续迁移到 Linux，可以沿用同一思路：

1. `npm run build`
2. 将 `frontend/dist` 发布到站点目录
3. 将 `/api/` 代理到 `9000`
4. 将 `/monitor/` 代理到 `9100`
5. 后端服务通过 `systemd` 或其他进程管理工具启动

## 四、排查命令

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

## 五、部署验收清单

- [ ] `database/init.sql` 已执行
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] `user-service`、`activity-service`、`gateway-service`、`monitor-service` 已启动
- [ ] `frontend/dist` 已生成
- [ ] Nginx 已成功加载配置
- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
