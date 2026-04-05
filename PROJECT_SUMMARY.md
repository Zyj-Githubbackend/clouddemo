# 项目交付摘要

## 当前状态

项目已经具备完整的本机运行链路：

- 后端微服务可独立启动
- 前端可开发模式运行，也可构建后交给 Nginx 托管
- `monitor-service` 可通过 `/monitor/` 访问
- 已提供本机 Nginx 配置片段
- Docker Compose 已可直接在本机发布整套服务
- 本机 Jenkins 已可在构建后调用 Docker Compose 完成自动发布

## 已交付模块

| 模块 | 说明 |
|------|------|
| `gateway-service` | 统一入口、鉴权、转发 |
| `user-service` | 注册登录、资料、时长 |
| `activity-service` | 活动、报名、签到、核销、AI 文案 |
| `monitor-service` | Spring Boot Admin 监控中心 |
| `frontend` | Vue 3 前端页面 |
| `database/init.sql` | 数据库初始化和示例数据 |
| `deploy/nginx/` | 本机 Nginx 配置 |
| `deploy/docker/` | 后端通用 Docker 构建文件 |
| `services/*/Dockerfile` | 每个微服务独立镜像构建文件 |
| `docker-compose.yml` | 一键启动整套容器环境 |

## 当前推荐访问方式

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- 开发模式前端：`http://localhost:3000`
- Docker 模式前端：`http://localhost:8081/`
- Docker 网关直连：`http://localhost:9001`
- Docker 监控直连：`http://localhost:9101`
- Docker Nacos：`http://localhost:8849/nacos`

## 最近补充的业务能力

- 活动支持多图上传、编辑和详情轮播展示
- 用户可在“我的志愿足迹”页面导出本人已核销志愿时长及活动列表 Excel
- Docker 前端映射已调整为 `8081:80`
- 本机 Jenkins 可通过 `Execute Windows batch command` 先执行 `mvn -B test`，再触发 `docker compose up -d --build`

## 关键配置对齐

- 前端请求基址：`/api`
- 网关端口：`9000`
- 监控端口：`9100`
- `monitor-service` 已启用反向代理兼容配置

## 建议阅读顺序

1. [README.md](README.md)
2. [QUICKSTART.md](QUICKSTART.md)
3. [DEPLOY.md](DEPLOY.md)
4. [API_TEST.md](API_TEST.md)

## 备注

旧版文档中的大段“完成度报告”、重复目录树、重复启动说明已经收敛到对应文档中，避免同一信息在多个文件里重复维护。
