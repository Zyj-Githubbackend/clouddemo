# 目录结构

```text
cloud-demo/
├─ database/
│  └─ init.sql
├─ deploy/
│  ├─ docker/
│  │  └─ backend.Dockerfile
│  └─ nginx/
│     ├─ cloud-demo.conf
│     └─ cloud-demo.local.conf
├─ frontend/
│  ├─ src/
│  │  ├─ api/
│  │  ├─ components/
│  │  ├─ router/
│  │  ├─ store/
│  │  ├─ utils/
│  │  └─ views/
│  ├─ README.md
│  ├─ QUICKSTART.md
│  └─ PROJECT_COMPLETE.md
├─ services/
│  ├─ common/
│  ├─ gateway-service/
│  ├─ user-service/
│  ├─ activity-service/
│  └─ monitor-service/
├─ API_TEST.md
├─ ARCHITECTURE.md
├─ CHECKLIST.md
├─ DEPLOY.md
├─ PROJECT_SUMMARY.md
├─ QUICKSTART.md
└─ README.md
```

## 重点目录说明

### `database/`

- 保存数据库初始化脚本
- 当前只有一个入口文件：`init.sql`

### `deploy/nginx/`

- 保存仓库内维护的 Nginx 配置
- `cloud-demo.local.conf` 用于本机部署
- `cloud-demo.conf` 适合作为更通用的参考版本

### `deploy/docker/`

- 保存后端通用 Docker 构建文件
- `backend.Dockerfile` 通过 `MODULE` 参数构建不同微服务镜像

### `services/`

- `common/`：公共工具和统一返回结构
- `gateway-service/`：统一入口、JWT 鉴权、转发
- `user-service/`：用户相关业务
- `activity-service/`：活动和报名相关业务
- `monitor-service/`：监控中心

### `frontend/`

- Vue 3 前端工程
- `src/views/` 存放页面
- `src/api/` 存放接口封装
- `src/utils/request.js` 定义统一请求实例

## 文档分工

- `README.md`：总入口
- `QUICKSTART.md`：本机快速运行
- `DEPLOY.md`：部署和 Nginx
- `ARCHITECTURE.md`：架构说明
- `API_TEST.md`：接口测试
- `CHECKLIST.md`：验收项
