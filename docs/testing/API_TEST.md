# API 测试说明

## 1. 基础地址

开发模式直连网关：

- `http://localhost:9000`

Nginx 模式：

- 页面入口：`http://localhost/`
- API 前缀：`http://localhost/api`

Docker 模式：

- 页面入口：`http://localhost:8081/`
- API 前缀：`http://localhost:8081/api`

说明：以下示例默认使用“开发模式直连网关” `http://localhost:9000`。如果在 Docker 单栈模式下测试，请把示例中的 `http://localhost:9000` 替换为 `http://localhost:8081/api`。

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
- `GET /announcement/home`
- `GET /announcement/list`
- `GET /announcement/image`
- `GET /announcement/attachment`

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

## 7. 公告接口

### 首页公告

```bash
curl "http://localhost:9000/announcement/home?limit=5"
```

### 公告列表

```bash
curl "http://localhost:9000/announcement/list?page=1&size=10"
```

### 公告详情

```bash
curl http://localhost:9000/announcement/1 \
  -H "Authorization: Bearer <token>"
```

### 公告图片

```bash
curl "http://localhost:9000/announcement/image?objectKey=announcements/example.jpg" --output announcement.jpg
```

### 公告附件

```bash
curl "http://localhost:9000/announcement/attachment?objectKey=announcements/attachments/example.pdf&fileName=example.pdf" --output example.pdf
```

## 8. 意见反馈接口

反馈分类：

- `QUESTION`
- `SUGGESTION`
- `BUG`
- `COMPLAINT`
- `OTHER`

反馈状态：

- `OPEN`
- `REPLIED`
- `CLOSED`
- `REJECTED`

反馈优先级：

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

### 创建反馈

```bash
curl -X POST http://localhost:9000/feedback \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d "{\"title\":\"页面建议\",\"category\":\"SUGGESTION\",\"content\":\"希望活动详情页增加更多提示\",\"attachments\":[]}"
```

### 我的反馈列表

```bash
curl "http://localhost:9000/feedback/my?page=1&size=10&status=OPEN" \
  -H "Authorization: Bearer <token>"
```

### 反馈详情

```bash
curl http://localhost:9000/feedback/1 \
  -H "Authorization: Bearer <token>"
```

### 追加回复

```bash
curl -X POST http://localhost:9000/feedback/1/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d "{\"content\":\"补充一条反馈说明\",\"attachments\":[]}"
```

### 关闭自己的反馈

```bash
curl -X POST http://localhost:9000/feedback/1/close \
  -H "Authorization: Bearer <token>"
```

### 上传与下载反馈附件

```bash
curl -X POST http://localhost:9000/feedback/attachments \
  -H "Authorization: Bearer <token>" \
  -F "file=@D:/files/feedback.png"

curl "http://localhost:9000/feedback/attachments?objectKey=feedback/tmp/1/example.png&fileName=feedback.png" \
  -H "Authorization: Bearer <token>" \
  --output feedback.png
```

上传返回的 `attachmentKey`、`fileName`、`contentType`、`fileSize`、`fileType` 可放入创建反馈或追加回复的 `attachments` 数组。每条消息最多 6 个附件。

### 管理员反馈列表与详情

```bash
curl "http://localhost:9000/feedback/admin/list?page=1&size=10&status=OPEN&category=QUESTION&priority=NORMAL&keyword=页面" \
  -H "Authorization: Bearer <admin-token>"

curl http://localhost:9000/feedback/admin/1 \
  -H "Authorization: Bearer <admin-token>"
```

### 管理员回复、关闭、驳回和调整优先级

```bash
curl -X POST http://localhost:9000/feedback/admin/1/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"content\":\"已收到，我们会继续跟进\",\"attachments\":[]}"

curl -X POST http://localhost:9000/feedback/admin/1/priority \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"priority\":\"HIGH\"}"

curl -X POST http://localhost:9000/feedback/admin/1/close \
  -H "Authorization: Bearer <admin-token>"

curl -X POST http://localhost:9000/feedback/admin/1/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"reason\":\"该问题不属于平台处理范围\"}"
```

## 9. 报名相关接口

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

## 10. 管理员活动管理接口

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

## 11. 管理员报名管理接口

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

## 12. 管理员公告管理接口

### 管理员公告列表

```bash
curl "http://localhost:9000/announcement/admin/list?page=1&size=10" \
  -H "Authorization: Bearer <admin-token>"
```

### 发布公告

```bash
curl -X POST http://localhost:9000/announcement/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"title\":\"测试公告\",\"content\":\"公告内容\",\"activityId\":1,\"activityIds\":[1,2],\"status\":\"PUBLISHED\",\"sortOrder\":10,\"imageKeys\":[\"announcements/test-1.jpg\"],\"attachments\":[]}"
```

### 编辑公告

```bash
curl -X PUT http://localhost:9000/announcement/admin/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"title\":\"更新后的公告\",\"content\":\"更新后的公告内容\",\"activityId\":1,\"activityIds\":[1],\"status\":\"PUBLISHED\",\"sortOrder\":20,\"imageKeys\":[],\"attachments\":[]}"
```

### 下线与重新发布公告

```bash
curl -X POST http://localhost:9000/announcement/admin/1/offline \
  -H "Authorization: Bearer <admin-token>"

curl -X POST http://localhost:9000/announcement/admin/1/publish \
  -H "Authorization: Bearer <admin-token>"
```

### 删除公告

```bash
curl -X DELETE http://localhost:9000/announcement/admin/1 \
  -H "Authorization: Bearer <admin-token>"
```

### 上传公告图片

```bash
curl -X POST http://localhost:9000/announcement/admin/image \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@D:/images/announcement.png"
```

### 上传公告附件

```bash
curl -X POST http://localhost:9000/announcement/admin/attachment \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@D:/files/notice.pdf"
```

上传返回的 `attachmentKey`、`fileName`、`contentType`、`fileSize` 可放入发布或编辑公告的 `attachments` 数组。

## 13. AI 与图片接口

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

## 14. Nginx 场景验证

```bash
curl http://localhost/
curl "http://localhost/api/activity/list?page=1&size=10"
curl "http://localhost/api/announcement/home?limit=5"
curl http://localhost/monitor/
curl http://localhost/.well-known/oauth-authorization-server
curl -i http://localhost/mcp
```

## 15. 常见结果判断

- `9000` 通、`/api` 不通：Nginx API 代理问题
- `9100` 通、`/monitor/` 不通：监控代理问题
- `/.well-known/*` 不通：MCP OAuth 元数据代理问题
- `/mcp` 返回 `401`：未登录时属正常现象
- 登录 401：Token 缺失、过期或格式错误
- 登录 503：网关未找到下游服务，优先检查 Nacos 注册
