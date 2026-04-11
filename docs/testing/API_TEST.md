# API 测试说明

## 1. 基础地址

开发模式直连网关：

- `http://localhost:9000`

Nginx 模式：

- 页面入口：`http://localhost/`
- API 前缀：`http://localhost/api`

Docker 模式：

- 页面入口：`http://localhost:8081/`
- 网关直连：`http://localhost:9001`

## 2. 统一返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 3. 公开接口

当前网关白名单包括：

- `POST /user/login`
- `POST /user/register`
- `GET /activity/list`
- `GET /activity/image`

其余接口默认需要 `Authorization: Bearer <token>`。

## 4. 登录与注册

### 登录

```bash
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

成功后返回 `token` 与 `userInfo`。

### 注册

```bash
curl -X POST http://localhost:9000/user/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test001\",\"password\":\"password123\",\"realName\":\"测试用户\",\"studentNo\":\"2024999\"}"
```

## 5. 用户接口

### 当前用户信息

```bash
curl http://localhost:9000/user/info \
  -H "Authorization: Bearer <token>"
```

### 修改个人资料

```bash
curl -X PUT http://localhost:9000/user/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d "{\"realName\":\"张三\",\"studentNo\":\"2023001\",\"phone\":\"13800000000\",\"email\":\"zhangsan@example.com\"}"
```

### 修改密码

```bash
curl -X PUT http://localhost:9000/user/updatePassword \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d "{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}"
```

### 管理员查看志愿时长

```bash
curl "http://localhost:9000/user/admin/hours?keyword=student01" \
  -H "Authorization: Bearer <admin-token>"
```

## 6. 活动接口

### 活动列表

```bash
curl "http://localhost:9000/activity/list?page=1&size=10"
```

支持筛选参数：

- `status`：`RECRUITING` / `COMPLETED` / `CANCELLED`
- `category`
- `recruitmentPhase`：`NOT_STARTED` / `RECRUITING` / `ENDED`

示例：

```bash
curl "http://localhost:9000/activity/list?page=1&size=10&recruitmentPhase=RECRUITING"
```

### 活动详情

```bash
curl http://localhost:9000/activity/1 \
  -H "Authorization: Bearer <token>"
```

### 活动图片

```bash
curl "http://localhost:9000/activity/image?objectKey=activity-images/example.jpg" --output activity.jpg
```

## 7. 报名相关接口

### 报名活动

```bash
curl -X POST http://localhost:9000/activity/register/1 \
  -H "Authorization: Bearer <token>"
```

### 取消自己的报名

```bash
curl -X POST http://localhost:9000/activity/cancelRegistration/1 \
  -H "Authorization: Bearer <token>"
```

### 我的报名记录

```bash
curl http://localhost:9000/activity/myRegistrations \
  -H "Authorization: Bearer <token>"
```

### 导出我的已核销志愿记录

```bash
curl -L http://localhost:9000/activity/myRegistrations/exportConfirmed \
  -H "Authorization: Bearer <token>" \
  --output confirmed-hours.xlsx
```

## 8. 管理员活动管理接口

### 创建活动

```bash
curl -X POST http://localhost:9000/activity/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"title\":\"测试活动\",\"description\":\"活动说明\",\"location\":\"图书馆\",\"maxParticipants\":20,\"volunteerHours\":2.5,\"startTime\":\"2026-04-20T09:00:00\",\"endTime\":\"2026-04-20T12:00:00\",\"registrationStartTime\":\"2026-04-10T09:00:00\",\"registrationDeadline\":\"2026-04-19T18:00:00\",\"category\":\"校园服务\",\"imageKeys\":[\"activity-images/test-1.jpg\",\"activity-images/test-2.jpg\"]}"
```

### 编辑活动

```bash
curl -X PUT http://localhost:9000/activity/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"title\":\"更新后的活动标题\",\"description\":\"更新后的说明\",\"location\":\"图书馆二楼\",\"maxParticipants\":30,\"volunteerHours\":3,\"startTime\":\"2026-04-20T09:00:00\",\"endTime\":\"2026-04-20T13:00:00\",\"registrationStartTime\":\"2026-04-10T09:00:00\",\"registrationDeadline\":\"2026-04-19T18:00:00\",\"category\":\"校园服务\",\"imageKeys\":[\"activity-images/test-1.jpg\"]}"
```

### 取消活动

```bash
curl -X POST http://localhost:9000/activity/1/cancel \
  -H "Authorization: Bearer <admin-token>"
```

### 结项活动

```bash
curl -X POST http://localhost:9000/activity/1/complete \
  -H "Authorization: Bearer <admin-token>"
```

### 删除活动

```bash
curl -X DELETE http://localhost:9000/activity/1 \
  -H "Authorization: Bearer <admin-token>"
```

## 9. 管理员报名管理接口

### 查看全部报名记录

```bash
curl http://localhost:9000/activity/admin/registrations \
  -H "Authorization: Bearer <admin-token>"
```

### 查看某个活动的报名记录

```bash
curl "http://localhost:9000/activity/admin/registrations?activityId=14" \
  -H "Authorization: Bearer <admin-token>"
```

### 查看当前可签到活动

```bash
curl http://localhost:9000/activity/admin/checkInActivities \
  -H "Authorization: Bearer <admin-token>"
```

### 查看待核销活动

```bash
curl http://localhost:9000/activity/admin/endedActivities \
  -H "Authorization: Bearer <admin-token>"
```

### 执行签到

```bash
curl -X POST http://localhost:9000/activity/admin/checkIn/25 \
  -H "Authorization: Bearer <admin-token>"
```

### 核销时长

```bash
curl -X POST http://localhost:9000/activity/confirmHours/25 \
  -H "Authorization: Bearer <admin-token>"
```

## 10. AI 与图片接口

### AI 生成活动文案

```bash
curl -X POST http://localhost:9000/activity/ai/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"location\":\"图书馆\",\"category\":\"校园服务\",\"keywords\":\"整理书架,值班引导\",\"volunteerHours\":2.5}"
```

### 上传活动图片

```bash
curl -X POST http://localhost:9000/activity/admin/image \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@D:/images/activity.png"
```

## 11. Nginx 场景验证

```bash
curl http://localhost/
curl "http://localhost/api/activity/list?page=1&size=10"
curl http://localhost/monitor/
curl http://localhost/.well-known/oauth-authorization-server
curl -i http://localhost/mcp
```

## 12. 常见结果判断

- `9000` 通、`/api` 不通：Nginx API 代理问题
- `9100` 通、`/monitor/` 不通：监控代理问题
- `/.well-known/*` 不通：MCP OAuth 元数据代理问题
- `/mcp` 返回 `401`：未登录时属正常现象
- 登录 401：Token 缺失、过期或格式错误
- 登录 503：网关未找到下游服务，优先检查 Nacos 注册
