# 目录结构

```text
cloud-demo/
├─ compose.shared.yml
├─ compose.stack.yml
├─ compose.edge.yml
├─ docker-compose.yml        # 历史单架构 compose，兼容参考
├─ .env.example
├─ log/
│  ├─ a/
│  │  ├─ gateway-service/
│  │  ├─ user-service/
│  │  ├─ activity-service/
│  │  ├─ announcement-service/
│  │  ├─ feedback-service/
│  │  └─ monitor-service/
│  ├─ b/
│  │  ├─ gateway-service/
│  │  ├─ user-service/
│  │  ├─ activity-service/
│  │  ├─ announcement-service/
│  │  ├─ feedback-service/
│  │  └─ monitor-service/
│  ├─ edge/
│  │  └─ edge-nginx/
│  ├─ shared/
│  │  └─ mcp-service/
│  └─ README.md
├─ docs/
│  ├─ architecture/
│  ├─ deploy/
│  ├─ ui-page-inventory.md
│  ├─ stitch-page-spec.md
│  ├─ mcp/
│  ├─ observability/
│  ├─ overview/
│  ├─ setup/
│  ├─ testing/
│  └─ README.md
├─ database/
│  ├─ init.sql
│  └─ migrations/
├─ deploy/
│  ├─ docker/
│  │  └─ backend.Dockerfile
│  ├─ edge-nginx/
│  ├─ observability/
│  ├─ nginx/
│  │  └─ cloud-demo.local.conf
│  ├─ stack-a.env
│  ├─ stack-b.env
│  ├─ up-shared.ps1 / up-shared.sh
│  ├─ up-stack-a.ps1 / up-stack-a.sh
│  ├─ up-stack-b.ps1 / up-stack-b.sh
│  ├─ up-edge.ps1 / up-edge.sh
│  └─ deploy-all.ps1 / deploy-all.sh
├─ frontend2/
│  ├─ dist/                 # 构建产物
│  ├─ src/
│  │  ├─ api/
│  │  ├─ components/
│  │  ├─ router/
│  │  ├─ store/
│  │  ├─ utils/
│  │  └─ views/
│  ├─ Dockerfile
│  ├─ nginx.docker.conf
│  ├─ package.json
│  └─ README.md
├─ scripts/
│  ├─ mcp-login.ps1
│  └─ mcp-print-token.ps1
├─ services/
│  ├─ common/
│  ├─ common-messaging/
│  ├─ activity-service/
│  ├─ announcement-service/
│  ├─ feedback-service/
│  ├─ gateway-service/
│  ├─ mcp-service/
│  ├─ monitor-service/
│  ├─ user-service/
│  └─ pom.xml
├─ pom.xml
├─ start-all.bat
├─ start-all.sh
└─ README.md
```

## 重点目录说明

### `services/`

- Maven 聚合工程所在目录
- `common/`：统一结果、异常、JWT 工具
- `common-messaging/`：RabbitMQ、outbox、消费幂等等跨服务消息基础设施
- `user-service/`：用户注册、登录、资料维护、时长统计
- `activity-service/`：活动、报名、签到、时长核销、AI 文案、图片上传
- `announcement-service/`：公告发布、首页公告、公告图片/附件上传和关联活动跳转
- `feedback-service/`：意见反馈工单、消息回复、附件上传和管理员处理
- `gateway-service/`：统一入口、JWT 鉴权、请求头注入
- `monitor-service/`：Spring Boot Admin 服务端
- `mcp-service/`：MCP Server 与 OAuth 端点

### `frontend2/`

- Vue 3 前端工程
- `src/views/` 保存用户端页面和管理员页面
- `src/api/` 保存接口封装
- `src/utils/request.js` 定义统一请求实例
- `dist/` 为 `npm run build` 后生成的静态文件目录

### `database/`

- `init.sql` 为数据库主初始化入口
- 包含建表、默认账号、默认活动与报名记录
- `migrations/` 保存已有数据库的增量升级脚本，例如公告表和反馈表

### `deploy/`

- `deploy/nginx/cloud-demo.local.conf`：本机 Nginx 代理配置
- `deploy/edge-nginx/`：Docker edge 入口镜像与 nginx 配置
- `deploy/observability/`：Prometheus、Promtail、Tempo、Loki、Grafana、OTel Collector 配置
- `deploy/stack-a.env` / `deploy/stack-b.env`：A/B 栈部署参数
- `deploy/up-*.ps1|sh`、`deploy/deploy-all.*`、`deploy/down-all.*`：分层启动与关闭脚本

### `log/`

- `log/shared/mcp-service/`：shared 层 MCP 日志
- `log/a/...`：A 栈各服务日志
- `log/b/...`：B 栈各服务日志
- `log/edge/edge-nginx/`：edge nginx 访问与错误日志

所有 Docker 运行期日志都集中到 `log/`，避免与 `services/` 下的源码目录混淆。

说明：当前 Loki/Promtail 主要通过 Docker 容器发现采集实例日志，`log/` 仍保留作为宿主机落盘日志与 edge 文件日志兜底。

## 文档分工

- `README.md`：总入口与最短说明
- `docs/setup/QUICKSTART.md`：本机快速运行
- `docs/deploy/DEPLOY.md`：A/B 双栈 Docker 部署与排障说明
- `docs/observability/OBSERVABILITY.md`：指标、日志、链路与按实例观察说明
- `docs/architecture/ARCHITECTURE.md`：系统结构与调用链
- `docs/testing/API_TEST.md`：接口验证样例
- `docs/mcp/MCP_CONNECTION.md`：MCP 接入与 OAuth 登录
- `frontend2/README.md`、`docs/ui-page-inventory.md`、`docs/stitch-page-spec.md`：前端专项说明
