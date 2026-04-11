# 前端快速启动

## 1. 技术栈

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Element Plus
- ECharts
- dayjs

## 2. 开发模式

```bash
cd frontend
npm install
npm run dev
```

访问：

- `http://localhost:3000`

前提：

- `gateway-service` 已启动在 `9000`
- `user-service` 与 `activity-service` 已正常提供接口

开发模式下，Vite 会把 `/api` 代理到 `http://localhost:9000`。

## 3. 构建模式

```bash
cd frontend
npm run build
```

构建产物目录：

- `frontend/dist`

构建后建议通过本机 Nginx 访问：

- `http://localhost/`

## 4. 主要页面

用户侧：

- `/login`
- `/register`
- `/home`
- `/activities`
- `/activity/:id`
- `/my`
- `/profile`

管理员侧：

- `/admin/activities`
- `/admin/create`
- `/admin/checkin`
- `/admin/confirm`
- `/admin/hours`

## 5. 当前前端能力

- 登录与注册
- 活动列表筛选
- 活动详情展示
- 动态显示招募阶段与活动阶段
- 我的志愿足迹列表
- 取消未开始活动的报名
- 导出已核销志愿足迹 Excel
- 管理端活动创建、编辑、取消、结项
- 管理端多图上传
- 管理端签到、时长核销、志愿时长统计

## 6. 关键约定

- 请求前缀统一为 `/api`
- 图片展示统一走 `/api/activity/image`
- 开发模式由 Vite 代理 `/api`
- 部署模式由 Nginx 代理 `/api/`

## 7. 验证项

- [ ] 登录页可打开
- [ ] 活动列表可加载
- [ ] 登录后可跳转到首页
- [ ] 我的志愿足迹可展示报名记录
- [ ] 已核销记录可导出 Excel
- [ ] 管理员可进入后台页面
- [ ] 管理员可上传活动图片

## 8. 常见问题

### `npm install` 失败

```bash
npm cache clean --force
npm install
```

### 页面能打开但接口报错

优先检查：

- `gateway-service` 是否运行在 `9000`
- `/api` 代理是否生效
- 浏览器是否携带本地 `token`

### 刷新页面 404

说明当前没有通过带有 `try_files ... /index.html` 的 Nginx 配置访问，请改用仓库内提供的 Nginx 入口。
