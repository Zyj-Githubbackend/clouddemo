# 项目交付摘要

## 当前状态

项目已经具备完整的本机、Docker 单栈和 Kubernetes 部署链路：

- 后端微服务可独立启动
- 前端可开发模式运行，也可构建后交给 Nginx 托管
- `monitor-service` 支持通过 `/monitor/` 反向代理访问
- `mcp-service` 支持通过 `/mcp` 对外提供 MCP 能力
- 已接入基础可观测栈：Prometheus / Grafana / Loki / Tempo / OTel Collector
- 剩余同步高风险链路已接入 Resilience4j

## 已交付模块

| 模块 | 说明 |
| --- | --- |
| `services/common` | 统一结果结构、异常与 JWT 工具 |
| `services/user-service` | 注册登录、资料维护、志愿时长查询 |
| `services/activity-service` | 活动管理、报名、签到、时长核销、AI 文案、图片上传、Excel 导出 |
| `services/announcement-service` | 公告发布、编辑、下线、图片/附件上传、关联活动和首页公告展示 |
| `services/feedback-service` | 意见反馈创建、回复、关闭、驳回、优先级和附件管理 |
| `services/gateway-service` | 统一入口、JWT 鉴权、请求头透传 |
| `services/monitor-service` | Spring Boot Admin 监控中心 |
| `services/mcp-service` | 独立 MCP Server，封装用户、活动、公告与意见反馈相关工具 |
| `frontend2` | Vue 3 前端页面与后台管理界面 |
| `deploy/common/bootstrap-db.sql` | 本机、Docker、Kubernetes 共用的数据库结构、默认账号与示例数据 |
| `deploy/local` | 本机部署配置 |
| `docker-compose.yml` / `deploy/docker` | 默认 Docker 单栈部署配置 |
| `deploy/k8s` | Kubernetes 部署配置 |

## 推荐访问方式

### 本机 Nginx 模式

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`
- 开发模式前端：`http://localhost:3000`

### Docker 单栈

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:9090`

### Kubernetes

- 前台：`http://cloud-demo.local:18081/`
- Grafana：`http://grafana.cloud-demo.local:18081/`
- Prometheus：`http://prometheus.cloud-demo.local:18081/`

## 示例数据说明

- 默认账号：`admin` 与 `student01` 到 `student10`
- 默认密码：`password123`
- 默认活动数据覆盖“未开始、招募中、招募结束、进行中、已完成、已取消”等场景

## 建议阅读顺序

1. [README.md](../../README.md)
2. [快速开始](../setup/QUICKSTART.md)
3. [部署说明](../deploy/DEPLOY.md)
4. [API 测试](../testing/API_TEST.md)
5. [MCP 连接说明](../mcp/MCP_CONNECTION.md)
