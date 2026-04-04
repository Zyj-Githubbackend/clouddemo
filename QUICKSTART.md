# 快速开始指南

本文档只保留一条最短可用路径：在本机启动全部服务，并通过 Nginx 统一访问前台和监控后台。

## 前置条件

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis
- Nacos 2.x
- Nginx

## 步骤 1：初始化数据库

```bash
mysql -u root -p < database/init.sql
```

注意：`database/init.sql` 会先执行 `DROP DATABASE IF EXISTS volunteer_platform`，请不要在已有正式数据的库上直接执行。

## 步骤 2：启动 Redis 和 Nacos

Redis：

```bash
redis-server
```

Nacos：

```bash
cd nacos/bin
startup.cmd -m standalone
```

启动后访问 `http://localhost:8848/nacos`，默认账号密码均为 `nacos`。

## 步骤 3：编译后端

```bash
mvn clean install -DskipTests
```

## 步骤 4：启动后端服务

建议顺序：

1. `UserApplication`
2. `ActivityApplication`
3. `GatewayApplication`
4. `MonitorApplication`

如果使用命令行，也可以分别进入各模块执行：

```bash
mvn spring-boot:run
```

端口约定：

- `8100`：user-service
- `8200`：activity-service
- `9000`：gateway-service
- `9100`：monitor-service

## 步骤 5：前端运行方式

开发模式：

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:3000`

Nginx 模式：

```bash
cd frontend
npm install
npm run build
```

然后使用 [deploy/nginx/cloud-demo.local.conf](deploy/nginx/cloud-demo.local.conf) 中的规则启动本机 Nginx。

## 步骤 6：验证访问地址

本机验证：

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- 网关直连：`http://localhost:9000`
- Nacos：`http://localhost:8848/nacos`

同校园网访问：

- 使用本机当前 IPv4 地址访问，例如 `http://你的局域网IP/`
- 监控后台为 `http://你的局域网IP/monitor/`

如果同学无法访问，优先检查：

- Windows 防火墙是否放行 `80` 端口
- 电脑是否仍连接校园网
- Nginx 是否在监听 `80`

## 快速验证命令

```bash
curl http://127.0.0.1/
curl http://127.0.0.1:9000/activity/list?page=1&size=10
curl http://127.0.0.1:9100/
```

登录接口验证：

```bash
curl -X POST http://127.0.0.1:9000/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

## 常见问题

### 1. 登录返回 503

- 检查 `user-service` 是否已注册到 Nacos
- 检查 `gateway-service` 是否正常启动

### 2. 访问 `/monitor/` 失败

- 检查 `monitor-service` 是否运行在 `9100`
- 检查 Nginx 是否已加载 `/monitor/` 代理配置

### 3. 本机能访问，同学不能访问

- 检查 Windows 防火墙
- 检查是否使用了正确的校园网 IPv4 地址
