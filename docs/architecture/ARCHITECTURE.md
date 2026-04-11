# 架构说明

## 1. 总体结构

项目采用前后端分离 + 微服务架构。

### 本机 Nginx 模式

```text
Browser
  │
  ▼
Nginx :80
  ├─ /                -> frontend/dist
  ├─ /api/**          -> gateway-service:9000
  ├─ /monitor/**      -> monitor-service:9100
  ├─ /.well-known/**  -> mcp-service:9300
  ├─ /authorize       -> mcp-service:9300
  ├─ /token           -> mcp-service:9300
  ├─ /register        -> mcp-service:9300
  └─ /mcp**           -> mcp-service:9300
                         │
                         ▼
                 gateway-service
                  ├─ user-service
                  ├─ activity-service
                  └─ announcement-service
```

### 依赖基础设施

- MySQL：业务数据
- Redis：活动库存与报名并发控制
- Nacos：服务注册与发现
- MinIO：活动图片对象存储
- Spring Boot Admin：服务监控

## 2. 服务职责

### `gateway-service`

- 统一入口
- 校验业务 JWT
- 向下游透传 `X-User-Id`、`X-Username`、`X-User-Role`
- 为请求补充 `X-Trace-Id`
- 按 `/user/**`、`/activity/**` 路由到对应服务

当前白名单接口：

- `/user/login`
- `/user/register`
- `/activity/list`
- `/activity/image`

### `user-service`

- 用户注册、登录
- 查询当前用户资料
- 修改个人资料
- 修改密码
- 管理员查看志愿时长汇总

### `activity-service`

- 活动列表与详情
- 活动创建、编辑、取消、结项、删除
- 报名与取消报名
- 管理员签到与时长核销
- AI 活动文案生成
- MinIO 图片上传与图片读取
- 导出“我的志愿足迹”Excel

### `announcement-service`

- 首页公告列表与公告详情
- 管理员发布、编辑、下线、删除公告
- 公告图片上传与读取
- 公告可关联具体活动详情页

### `monitor-service`

- Spring Boot Admin Server
- 展示服务健康状态与监控面板
- 已适配 `/monitor/` 前缀代理访问

### `mcp-service`

- 独立 MCP Server
- 暴露 Streamable HTTP MCP 端点 `/mcp`
- 提供 OAuth 元数据、客户端注册、授权码登录与 token 交换
- 将 MCP 工具请求转调现有网关接口

## 3. 核心调用链

### 普通 Web 请求

1. 浏览器访问 Nginx。
2. Nginx 将 `/api/**` 转发给 `gateway-service`。
3. 网关校验 JWT，并写入用户请求头。
4. 网关转发到 `user-service` 或 `activity-service`。
5. 服务返回统一 `Result<T>` 结构。

### 活动图片链路

1. 管理员通过 `/activity/admin/image` 上传图片。
2. `activity-service` 将对象写入 MinIO。
3. 前端通过 `/api/activity/image?objectKey=...` 读取图片。
4. `activity-service` 负责读取对象流并返回给浏览器。

### MCP OAuth 链路

1. MCP 客户端访问 `/.well-known/oauth-protected-resource` 与 `/.well-known/oauth-authorization-server`。
2. 客户端通过 `/register` 注册 OAuth 客户端。
3. 用户在 `/authorize` 页面使用平台账号登录。
4. `mcp-service` 调用 `/user/login` 换取业务 JWT。
5. 客户端通过 `/token` 交换 access token。
6. 后续访问 `/mcp` 时，`mcp-service` 直接复用该业务 JWT 调用网关接口。

## 4. 认证设计

### Web 业务接口

- 令牌头：`Authorization: Bearer <token>`
- 网关透传请求头：
  - `X-User-Id`
  - `X-Username`
  - `X-User-Role`
  - `X-Trace-Id`

### MCP 接口

- `/mcp` 路径受 `McpAccessTokenFilter` 保护
- `/mcp/auth/login` 为手动 token 辅助接口
- 管理员工具最终仍由业务 JWT 中的 `role=ADMIN` 控制

## 5. 活动与报名规则

### 活动状态

数据库中持久化的活动状态目前为：

- `RECRUITING`
- `COMPLETED`
- `CANCELLED`

### 招募阶段

活动列表支持 `recruitmentPhase` 筛选，运行时动态根据当前时间计算：

- `NOT_STARTED`
- `RECRUITING`
- `ENDED`

前端页面额外会根据活动时间展示：

- 活动未开始
- 活动进行中
- 活动已结束
- 活动已取消

### 报名并发控制

活动报名使用 Redis 做库存控制：

1. 创建活动时初始化活动库存 key。
2. 报名前先扣减 Redis 库存。
3. 扣减成功后再写入数据库。
4. 若写库失败或名额不足，则回滚库存。

这样可降低高并发场景下的超卖风险。

## 6. 图片与导出设计

### 活动图片

- 数据库存储字段：`vol_activity.image_key`
- 逻辑上支持多图，多个对象键以逗号分隔
- 接口返回兼容字段：
  - `imageKey` / `imageUrl`：首图
  - `imageKeys` / `imageUrls`：完整图片列表

### 志愿足迹导出

- 导出接口：`GET /activity/myRegistrations/exportConfirmed`
- 返回 Excel 文件
- 内容包括：
  - 已核销活动数
  - 已核销总时长
  - 已核销活动明细

## 7. 监控与运维

- `monitor-service` 对外端口：`9100`
- Docker 模式下宿主机映射：`9101`
- 通过 Nacos discovery 自动加载已注册的 `user-service`、`activity-service`、`announcement-service`、`gateway-service`
- `mcp-service` 已接入 Spring Boot Admin Client
- 本机与 Docker 模式均推荐通过前端 Nginx 入口访问监控页面，而不是直接暴露内部端口作为最终入口
