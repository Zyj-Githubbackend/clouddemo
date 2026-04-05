# 架构说明

## 总体结构

项目采用前后端分离 + 微服务架构：

```text
Vue 3 Frontend
   │
   ▼
Nginx
 ├─ /         -> frontend/dist
 ├─ /api/     -> gateway-service
 └─ /monitor/ -> monitor-service
   │
   ▼
gateway-service
 ├─ user-service
 └─ activity-service
```

基础设施：

- MySQL：业务数据
- Redis：报名库存与并发控制
- Nacos：注册与发现
- Spring Boot Admin：服务监控

## 服务职责

### gateway-service

- 统一入口
- JWT 解析与鉴权
- 将用户信息写入请求头
- 按路径转发到下游服务

### user-service

- 注册、登录
- 用户资料维护
- 志愿时长查询
- 管理员查看时长汇总
- 用户在“我的志愿足迹”页面导出本人已核销时长汇总与活动明细

### activity-service

- 活动创建、编辑、取消、结项
- 活动列表和详情
- 活动图片多图上传、存储与详情轮播展示
- 报名、签到、核销时长
- AI 生成活动文案

### monitor-service

- Spring Boot Admin Server
- 统一查看服务健康状态和监控面板

## 核心调用链

### 用户请求链路

1. 浏览器访问 Nginx
2. Nginx 将 `/api/` 转发到 `gateway-service`
3. 网关完成鉴权和请求头注入
4. 网关按路径转发到 `user-service` 或 `activity-service`
5. 服务返回统一 `Result<T>` 结构

### 监控链路

1. 浏览器访问 `/monitor/`
2. Nginx 转发到 `monitor-service:9100`
3. `monitor-service` 从注册中心发现各服务
4. 在监控后台展示服务状态

## 认证设计

- 登录接口：`POST /user/login`
- 令牌格式：`Authorization: Bearer <token>`
- 网关注入的请求头包括：
  - `X-User-Id`
  - `X-Username`
  - `X-User-Role`

公开接口当前主要包括：

- `/user/login`
- `/user/register`
- `/activity/list`

## 报名并发控制

活动报名使用 Redis 做防超卖控制：

1. 活动创建时初始化库存
2. 报名时先在 Redis 扣减库存
3. 扣减成功后再写入数据库
4. 失败时回滚库存

这样可以降低高并发下的超卖风险。

## 当前本机部署形态

当前仓库已经围绕本机 Nginx 部署做了对齐：

- 前端静态文件通过 Nginx 提供
- API 不再直接由前端访问 `9000`，而是走 `/api/`
- 监控后台通过 `/monitor/` 暴露
- `monitor-service` 已适配反向代理

对应配置文件：

- [deploy/nginx/cloud-demo.local.conf](deploy/nginx/cloud-demo.local.conf)

## 关键目录

- `services/`：后端服务
- `frontend/`：前端工程
- `database/init.sql`：数据库初始化脚本
- `deploy/nginx/`：Nginx 配置
