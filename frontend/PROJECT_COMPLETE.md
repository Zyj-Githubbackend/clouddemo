# 前端交付摘要

## 当前状态

前端已完成并与当前后端接口保持一致，支持两种运行方式：

- 开发模式：`npm run dev`
- 部署模式：`npm run build` 后由 Nginx 托管

## 页面范围

用户侧：

- 登录
- 注册
- 首页
- 活动列表
- 活动详情
- 个人中心

管理员侧：

- 活动管理
- 发布活动
- 活动签到
- 时长核销
- 志愿时长统计

## 当前接口约定

- 前端统一使用 `/api` 作为请求前缀
- 开发模式由 Vite 代理到 `9000`
- 部署模式由 Nginx 代理到 `9000`

## 当前访问入口

- 开发模式：`http://localhost:3000`
- 部署模式：`http://localhost/`

## 相关文档

- [README.md](README.md)
- [QUICKSTART.md](QUICKSTART.md)
- [../DEPLOY.md](../DEPLOY.md)
