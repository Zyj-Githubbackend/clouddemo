# 目录结构

```text
cloud-demo/
├─ docker-compose.yml       # 默认 Docker 单栈入口
├─ .env.example             # 默认 Docker 环境变量模板
├─ deploy/
│  ├─ common/
│  │  └─ bootstrap-db.sql
│  ├─ local/
│  │  ├─ README.md
│  │  └─ nginx/cloud-demo.local.conf
│  ├─ docker/
│  │  ├─ docker-compose.yml
│  │  ├─ .env.example
│  │  ├─ up.ps1 / up.sh
│  │  ├─ down.ps1 / down.sh
│  │  ├─ edge-nginx/
│  │  └─ observability/
│  └─ k8s/
│     ├─ cloud-demo/
│     ├─ observability/
│     └─ scripts/
├─ docs/
├─ frontend2/
├─ services/
├─ scripts/
├─ pom.xml
└─ README.md
```

## 重点目录

### `services/`

- `common/`：统一结果、异常、JWT 工具
- `common-messaging/`：RabbitMQ、outbox、消费幂等等跨服务消息基础设施
- `user-service/`：用户注册、登录、资料维护、时长统计
- `activity-service/`：活动、报名、签到、时长核销、AI 文案、图片上传
- `announcement-service/`：公告发布、首页公告、公告图片/附件上传和关联活动跳转
- `feedback-service/`：意见反馈工单、消息回复、附件上传和管理员处理
- `gateway-service/`：统一入口、JWT 鉴权、请求头透传
- `monitor-service/`：Spring Boot Admin 服务端
- `mcp-service/`：MCP Server 与 OAuth 端点

### `deploy/`

部署目录现在只保留三种形态：

- `deploy/local/`：本机部署
- `deploy/docker/`：Docker 单栈部署，每个服务一个容器
- `deploy/k8s/`：Kubernetes 部署

`deploy/common/bootstrap-db.sql` 被本机、Docker 和 Kubernetes 共用。

### `docs/`

- `docs/setup/QUICKSTART.md`：快速开始
- `docs/deploy/DEPLOY.md`：三种部署方式
- `docs/observability/OBSERVABILITY.md`：指标、日志、链路说明
- `docs/architecture/ARCHITECTURE.md`：系统结构与调用链
- `docs/testing/API_TEST.md`：接口验证样例
- `docs/mcp/MCP_CONNECTION.md`：MCP 接入与 OAuth 登录
