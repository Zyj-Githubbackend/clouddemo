# API 测试说明

## 基础地址

开发模式直连网关：

- `http://localhost:9000`

Nginx 模式：

- 页面入口：`http://localhost/`
- API 前缀：`http://localhost/api`

推荐在浏览器场景下通过 Nginx 访问，在接口单测场景下直接请求 `9000`。

## 统一返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 1. 登录

```bash
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

成功后会返回 `token` 和 `userInfo`。

## 2. 注册

```bash
curl -X POST http://localhost:9000/user/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test001\",\"password\":\"password123\",\"realName\":\"测试用户\",\"studentNo\":\"2024999\"}"
```

## 3. 获取当前用户信息

```bash
curl http://localhost:9000/user/info \
  -H "Authorization: Bearer <token>"
```

## 4. 获取活动列表

```bash
curl "http://localhost:9000/activity/list?page=1&size=10"
```

也支持筛选参数：

- `status`
- `category`
- `recruitmentPhase`

示例：

```bash
curl "http://localhost:9000/activity/list?page=1&size=10&recruitmentPhase=RECRUITING"
```

## 5. 获取活动详情

```bash
curl http://localhost:9000/activity/1 \
  -H "Authorization: Bearer <token>"
```

## 6. 报名活动

```bash
curl -X POST http://localhost:9000/activity/register/1 \
  -H "Authorization: Bearer <token>"
```

## 7. 我的报名记录

```bash
curl http://localhost:9000/activity/myRegistrations \
  -H "Authorization: Bearer <token>"
```

## 8. 导出我的已核销志愿记录

```bash
curl -L http://localhost:9000/activity/myRegistrations/exportConfirmed \
  -H "Authorization: Bearer <token>" \
  --output confirmed-hours.xlsx
```

返回结果为 Excel 文件，包含：

- 已核销活动数
- 已核销总时长
- 已核销活动明细列表

## 9. 管理员创建活动

```bash
curl -X POST http://localhost:9000/activity/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d "{\"title\":\"测试活动\",\"location\":\"图书馆\",\"category\":\"校园服务\",\"imageKeys\":[\"activity-images/test-1.jpg\",\"activity-images/test-2.jpg\"]}"
```

创建和编辑活动时都支持通过 `imageKeys` 传多张图片，活动详情接口会返回：

- `imageKey` / `imageUrl`：首图，兼容旧页面
- `imageKeys` / `imageUrls`：完整图片列表

## 10. 管理员查看志愿时长

```bash
curl "http://localhost:9000/user/admin/hours" \
  -H "Authorization: Bearer <admin-token>"
```

## 11. 管理员核销时长

```bash
curl -X POST http://localhost:9000/activity/confirmHours/1 \
  -H "Authorization: Bearer <admin-token>"
```

## Nginx 场景验证

如果要验证 Nginx 是否已正确代理，可以使用：

```bash
curl http://localhost/
curl http://localhost/api/activity/list?page=1&size=10
curl http://localhost/monitor/
```

## 常见结果判断

- `9000` 通、`/api` 不通：Nginx 代理问题
- `9100` 通、`/monitor/` 不通：Nginx 监控代理问题
- 登录 401：Token 缺失、过期或格式错误
- 登录 503：网关未找到下游服务，优先检查 Nacos 注册
