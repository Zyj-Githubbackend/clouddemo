# 目录结构

```text
cloud-demo/
├─ docs/
│  ├─ architecture/
│  ├─ deploy/
│  ├─ frontend/
│  ├─ mcp/
│  ├─ overview/
│  ├─ setup/
│  ├─ testing/
│  └─ README.md
├─ database/
│  └─ init.sql
├─ deploy/
│  ├─ docker/
│  │  └─ backend.Dockerfile
│  └─ nginx/
│     └─ cloud-demo.local.conf
├─ frontend/
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
│  ├─ activity-service/
│  ├─ announcement-service/
│  ├─ gateway-service/
│  ├─ mcp-service/
│  ├─ monitor-service/
│  ├─ user-service/
│  └─ pom.xml
├─ activity-service/        # 运行期日志目录
├─ announcement-service/    # 运行期日志目录
├─ gateway-service/         # 运行期日志目录
├─ mcp-service/             # 运行期日志目录
├─ monitor-service/         # 运行期日志目录
├─ user-service/            # 运行期日志目录
├─ .env.example
├─ docker-compose.yml
├─ pom.xml
├─ start-all.bat
├─ start-all.sh
└─ README.md
```

## 重点目录说明

### `services/`

- Maven 聚合工程所在目录
- `common/`：统一结果、异常、JWT 工具
- `user-service/`：用户注册、登录、资料维护、时长统计
- `activity-service/`：活动、报名、签到、时长核销、AI 文案、图片上传
- `announcement-service/`：公告发布、首页公告、公告图片上传和关联活动跳转
- `gateway-service/`：统一入口、JWT 鉴权、请求头注入
- `monitor-service/`：Spring Boot Admin 服务端
- `mcp-service/`：MCP Server 与 OAuth 端点

### `frontend/`

- Vue 3 前端工程
- `src/views/` 保存用户端页面和管理员页面
- `src/api/` 保存接口封装
- `src/utils/request.js` 定义统一请求实例
- `dist/` 为 `npm run build` 后生成的静态文件目录

### `database/`

- `init.sql` 为数据库唯一初始化入口
- 包含建表、默认账号、默认活动与报名记录

### `deploy/nginx/`

- `cloud-demo.local.conf` 为本机 Nginx 代理配置
- 同时代理前台、API、监控后台与 MCP

### 根目录日志目录

- `user-service/`
- `activity-service/`
- `announcement-service/`
- `gateway-service/`
- `mcp-service/`
- `monitor-service/`

这些目录当前主要用于本机运行与 Docker 运行时挂载日志，和 `services/` 下的源码目录不是同一层职责。

## 文档分工

- `README.md`：总入口与最短说明
- `docs/setup/QUICKSTART.md`：本机快速运行
- `docs/deploy/DEPLOY.md`：本机、Docker、Jenkins 部署说明
- `docs/architecture/ARCHITECTURE.md`：系统结构与调用链
- `docs/testing/API_TEST.md`：接口验证样例
- `docs/mcp/MCP_CONNECTION.md`：MCP 接入与 OAuth 登录
- `docs/frontend/*`：前端专项说明
