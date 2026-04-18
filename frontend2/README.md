# 前端说明

前端基于 Vue 3 + Vite，开发模式通过 Vite 运行，部署模式通过 Nginx 托管 `frontend2/dist`。

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
frontend2/
├─ src/
│  ├─ api/
│  ├─ components/
│  ├─ router/
│  ├─ store/
│  ├─ utils/
│  └─ views/
├─ index.html
├─ nginx.docker.conf
├─ package.json
└─ vite.config.js
```

## 运行方式

### 开发模式

```bash
cd frontend2
npm install
npm run dev
```

访问：

- `http://localhost:3000`

开发模式下，Vite 会把 `/api` 代理到 `http://localhost:9000`。

### 生产构建

```bash
cd frontend2
npm run build
```

构建结果输出到 `frontend2/dist`。

## 路由范围

用户侧：

- `/login`
- `/register`
- `/home`
- `/activities`
- `/activity/:id`
- `/announcement/:id`
- `/feedback`
- `/my-feedback`
- `/feedback/:id`
- `/my`
- `/profile`

管理员侧：

- `/admin/activities`
- `/admin/announcements`
- `/admin/feedback`
- `/admin/create`
- `/admin/checkin`
- `/admin/confirm`
- `/admin/hours`

## 与后端的接口约定

- 用户接口：`/api/user/*`
- 活动接口：`/api/activity/*`
- 公告接口：`/api/announcement/*`
- 意见反馈接口：`/api/feedback/*`
- 活动图片读取：`/api/activity/image?objectKey=...`
- 公告图片与附件读取：`/api/announcement/image?objectKey=...`、`/api/announcement/attachment?objectKey=...`
- 反馈附件读取：`/api/feedback/attachments?objectKey=...`
- 请求实例定义在 `src/utils/request.js`

## 已对齐的业务能力

- 登录、注册、资料维护
- 首页公告、公告详情、公告附件与关联活动跳转
- 活动列表筛选与详情展示
- 报名活动与取消报名
- 志愿足迹记录展示
- 已核销记录 Excel 导出
- 意见反馈提交、列表、详情、回复、关闭和附件上传
- 管理端活动管理
- 多图上传与详情轮播展示
- 管理端签到、核销、时长统计
- 管理端公告图片/附件、多活动关联与发布状态管理
- 管理端反馈工单筛选、回复、关闭、驳回和优先级管理

## 与 Nginx 的关系

当前仓库本机部署的推荐访问方式：

- `/`：前端页面
- `/api/`：转发到网关
- `/monitor/`：转发到监控后台
- `/mcp*` 与 OAuth 元数据：转发到 `mcp-service`

相关配置：

- [../deploy/nginx/cloud-demo.local.conf](../deploy/nginx/cloud-demo.local.conf)
- [nginx.docker.conf](nginx.docker.conf)

## 常见问题

### 页面能打开但接口报错

优先检查：

- `gateway-service` 是否运行在 `9000`
- Vite 代理或 Nginx `/api/` 代理是否生效

### 刷新页面 404

部署模式下必须通过带有 `try_files ... /index.html` 的 Nginx 配置访问。
