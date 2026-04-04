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

5. 前端二选一

- 开发模式：`cd frontend && npm install && npm run dev`
- Nginx 模式：`cd frontend && npm run build`，然后启动 Nginx

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
