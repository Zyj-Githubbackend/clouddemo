# 前端快速启动

## 开发模式

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:3000`

前提：

- `gateway-service` 已启动在 `9000`
- `user-service` 和 `activity-service` 已注册到 Nacos

## 构建模式

```bash
cd frontend
npm run build
```

构建后由 Nginx 提供页面访问，默认本机入口为：

- `http://localhost/`

## 验证项

- [ ] 登录页可打开
- [ ] 活动列表可加载
- [ ] 登录后可跳转到首页
- [ ] 管理员可进入后台页面

## 常见问题

### 1. `npm install` 失败

```bash
npm cache clean --force
npm install
```

### 2. `npm run dev` 后页面空白

检查浏览器控制台和网络面板，重点确认 `/api` 请求是否成功。

### 3. 构建后刷新 404

说明当前不是通过 Nginx 的 SPA 回退配置访问，请改用本机 Nginx 入口。
