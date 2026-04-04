# 前端说明

前端基于 Vue 3 + Vite，开发模式通过 Vite 运行，部署模式通过 Nginx 托管 `frontend/dist`。

## 技术栈

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Element Plus
- ECharts
- dayjs

## 目录概览

```text
frontend/
├─ src/
│  ├─ api/
│  ├─ components/
│  ├─ router/
│  ├─ store/
│  ├─ utils/
│  └─ views/
├─ index.html
├─ package.json
└─ vite.config.js
```

## 开发模式

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:3000`

开发模式下，Vite 会把 `/api` 代理到 `http://localhost:9000`。

## 生产构建

```bash
cd frontend
npm run build
```

构建结果输出到 `frontend/dist`。

## 与 Nginx 的关系

当前仓库本机部署的访问方式是：

- `/`：前端页面
- `/api/`：转发到网关
- `/monitor/`：转发到监控后台

相关配置见 [../deploy/nginx/cloud-demo.local.conf](../deploy/nginx/cloud-demo.local.conf)。

## 主要页面

- `Login.vue`
- `Register.vue`
- `Home.vue`
- `ActivityList.vue`
- `ActivityDetail.vue`
- `MyCenter.vue`
- `views/admin/*`

## 关键文件

- `src/utils/request.js`：统一请求实例，`baseURL` 为 `/api`
- `src/router/index.js`：路由和鉴权守卫
- `src/api/user.js`：用户接口
- `src/api/activity.js`：活动接口

## 常见问题

### 页面能打开但接口报错

优先检查：

- `gateway-service` 是否运行在 `9000`
- Vite 代理或 Nginx `/api/` 代理是否生效

### 刷新页面 404

部署模式下必须通过 Nginx 的 `try_files ... /index.html` 支持 Vue Router history 模式。
