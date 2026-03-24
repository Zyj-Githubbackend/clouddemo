# 📡 API 接口测试文档

本文档提供所有后端 API 接口的详细说明和测试示例。

## 🌐 基础信息

- **API网关地址**：`http://localhost:9000`
- **认证方式**：JWT Bearer Token（网关 `AuthFilter` 白名单除外）
- **网关白名单（无需 Token）**：`/user/login`、`/user/register`、以及以 `/activity/list` 开头的路径（活动列表公开）
- **返回格式**：JSON

### 统一返回格式

```json
{
  "code": 200,
  "message": "成功",
  "data": { ... }
}
```

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或Token过期 |
| 403 | 权限不足 |
| 500 | 服务器内部错误 |

## 🔐 用户服务 (user-service)

### 1. 用户注册

**接口**：`POST /user/register`

**无需认证**

**请求示例**：
```bash
curl -X POST http://localhost:9000/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "realName": "测试用户",
    "studentNo": "2024999",
    "phone": "13800138888",
    "email": "testuser@university.edu"
  }'
```

**请求参数**：
```json
{
  "username": "string",       // 用户名，必填，唯一
  "password": "string",       // 密码，必填，6位以上
  "realName": "string",       // 真实姓名，必填
  "studentNo": "string",      // 学号，必填，唯一
  "phone": "string",          // 手机号，选填
  "email": "string"           // 邮箱，选填
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

### 2. 用户登录

**接口**：`POST /user/login`

**无需认证**

**请求示例**：
```bash
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**请求参数**：
```json
{
  "username": "string",  // 用户名，必填
  "password": "string"   // 密码，必填
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjE3MTIwMDAwMDB9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "studentNo": "2021001",
      "phone": "13800138000",
      "email": "admin@university.edu",
      "role": "ADMIN",
      "totalVolunteerHours": 0.00
    }
  }
}
```

### 3. 获取当前用户信息

**接口**：`GET /user/info`

**需要认证**

**请求示例**：
```bash
curl -X GET http://localhost:9000/user/info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 2,
    "username": "student01",
    "realName": "张三",
    "studentNo": "2021101",
    "phone": "13800138001",
    "email": "zhangsan@university.edu",
    "role": "VOLUNTEER",
    "totalVolunteerHours": 12.50
  }
}
```

### 4. 更新志愿时长（内部接口）

**接口**：`POST /user/internal/updateHours/{userId}?hours={hours}`

**仅服务间调用**

**说明**：此接口由 activity-service 通过 Feign 调用，用于核销时长时更新用户累计时长。

## 🎯 活动服务 (activity-service)

### 1. 活动列表（公开）

**接口**：`GET /activity/list`

**无需认证**

**请求参数**：
- `page`：页码（默认1）
- `size`：每页大小（默认10）
- `status`：活动状态（可选：`RECRUITING`/`ONGOING`/`COMPLETED`）
- `category`：活动类型（可选：学长火炬、书记驿站等）

**请求示例**：
```bash
# 获取第1页，每页10条
curl "http://localhost:9000/activity/list?page=1&size=10"

# 筛选招募中的活动
curl "http://localhost:9000/activity/list?status=RECRUITING"

# 筛选学长火炬类活动
curl "http://localhost:9000/activity/list?category=学长火炬"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "title": "学长火炬 - 新生入学引导",
        "description": "协助新生办理入学手续，解答疑问...",
        "location": "学校东门迎新点",
        "maxParticipants": 50,
        "currentParticipants": 25,
        "volunteerHours": 4.0,
        "startTime": "2024-09-01T08:00:00",
        "endTime": "2024-09-01T18:00:00",
        "registrationDeadline": "2024-08-25T23:59:59",
        "status": "RECRUITING",
        "category": "学长火炬",
        "createTime": "2024-08-01T10:00:00"
      }
    ]
  }
}
```

### 2. 活动详情

**接口**：`GET /activity/{id}`

**需要认证**

**请求示例**：
```bash
curl -X GET http://localhost:9000/activity/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "学长火炬 - 新生入学引导",
    "description": "协助新生办理入学手续，解答疑问，帮助新生尽快适应大学生活...",
    "location": "学校东门迎新点",
    "maxParticipants": 50,
    "currentParticipants": 25,
    "volunteerHours": 4.0,
    "startTime": "2024-09-01T08:00:00",
    "endTime": "2024-09-01T18:00:00",
    "registrationDeadline": "2024-08-25T23:59:59",
    "status": "RECRUITING",
    "category": "学长火炬",
    "createdBy": 1,
    "createTime": "2024-08-01T10:00:00",
    "updateTime": "2024-08-01T10:00:00"
  }
}
```

### 3. 创建活动（管理员）

**接口**：`POST /activity/create`

**需要管理员权限**

**请求示例**：
```bash
curl -X POST http://localhost:9000/activity/create \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "书记驿站 - 周末值班",
    "description": "在书记驿站值班，为同学们提供咨询服务",
    "location": "图书馆一楼书记驿站",
    "maxParticipants": 10,
    "volunteerHours": 3.0,
    "startTime": "2024-09-15T09:00:00",
    "endTime": "2024-09-15T17:00:00",
    "registrationDeadline": "2024-09-10T23:59:59",
    "category": "书记驿站"
  }'
```

**请求参数**：
```json
{
  "title": "string",                 // 活动标题，必填
  "description": "string",           // 活动描述，必填
  "location": "string",              // 活动地点，必填
  "maxParticipants": 10,             // 招募人数上限，必填
  "volunteerHours": 3.0,             // 志愿时长，必填
  "startTime": "2024-09-15T09:00",   // 开始时间，必填
  "endTime": "2024-09-15T17:00",     // 结束时间，必填
  "registrationDeadline": "2024-09-10T23:59", // 报名截止，必填
  "category": "string"               // 活动类型，必填
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "活动创建成功",
  "data": null
}
```

### 4. 报名活动

**接口**：`POST /activity/register/{activityId}`

**需要认证**

**请求示例**：
```bash
curl -X POST http://localhost:9000/activity/register/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "报名成功",
  "data": null
}
```

**可能的错误**：
- `400`：名额已满
- `400`：已报名过该活动
- `400`：报名截止时间已过

### 5. 我的报名记录

**接口**：`GET /activity/myRegistrations`

**需要认证**

**请求示例**：
```bash
curl -X GET http://localhost:9000/activity/myRegistrations \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "activityId": 1,
      "activityTitle": "学长火炬 - 新生入学引导",
      "location": "学校东门迎新点",
      "volunteerHours": 4.0,
      "startTime": "2024-09-01T08:00:00",
      "endTime": "2024-09-01T18:00:00",
      "status": "RECRUITING",
      "registrationTime": "2024-08-20T15:30:00",
      "hoursConfirmed": false
    }
  ]
}
```

### 6. AI生成活动描述（管理员）

**接口**：`POST /activity/ai/generate`

**需要管理员权限**

**说明**：后端使用 **DeepSeek**（OpenAI 兼容 `chat/completions`）。请在 **activity-service** 配置环境变量 **`DEEPSEEK_API_KEY`**，或见 `application.properties` 中 `ai.api.*`。未配置或调用失败时仍返回 **200**，内容为**本地模板生成的兜底文案**。

**请求示例**：
```bash
curl -X POST http://localhost:9000/activity/ai/generate \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "location": "校医院门口",
    "category": "爱心小屋",
    "keywords": "献血车, 爱心服务, 周六"
  }'
```

**请求参数**：
```json
{
  "location": "string",   // 活动地点
  "category": "string",   // 活动类型
  "keywords": "string"    // 关键词
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "生成成功",
  "data": "爱心小屋献血车活动\n\n活动时间：本周六上午9:00-12:00\n活动地点：校医院门口\n\n活动内容：\n1. 引导同学们有序献血\n2. 提供现场咨询服务\n3. 发放献血纪念品\n4. 维护现场秩序\n\n报名要求：\n- 身体健康，能够站立服务\n- 具有良好的沟通能力\n- 有爱心和责任心\n\n志愿时长：3小时\n\n让我们一起传递爱心，温暖你我！"
}
```

### 7a. 获取全部报名列表（管理员）

**接口**：`GET /activity/admin/registrations`  
**Query**：`activityId`（可选，传入则只查该活动下的报名）

**需要管理员权限**（请求头 `X-User-Role` 由网关在解析 JWT 后注入）

**请求示例**：
```bash
curl -X GET "http://localhost:9000/activity/admin/registrations" \
  -H "Authorization: Bearer ADMIN_TOKEN"

# 仅查看活动 id=1 的报名
curl -X GET "http://localhost:9000/activity/admin/registrations?activityId=1" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**响应示例**（字段与 `RegistrationVO` 一致，含联表用户信息）：
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "userId": 2,
      "activityId": 1,
      "activityTitle": "学长火炬 - 新生入学引导",
      "location": "学校东门迎新点",
      "volunteerHours": 4.0,
      "startTime": "2024-09-01T08:00:00",
      "registrationTime": "2024-08-20T15:30:00",
      "checkInStatus": 0,
      "hoursConfirmed": 0,
      "status": "REGISTERED",
      "username": "student01",
      "realName": "张三",
      "studentNo": "2021101",
      "phone": "13800138001"
    }
  ]
}
```

**说明**：仅返回 `status=REGISTERED` 的报名；数据来自 `vol_registration` 联表 `vol_activity`、`sys_user`（与 `activity-service` 使用同一 MySQL 库）。

### 7b. 获取指定活动的报名列表（管理员）

**接口**：`GET /activity/{activityId}/registrations`

**需要管理员权限**

**请求示例**：
```bash
curl -X GET http://localhost:9000/activity/1/registrations \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

响应格式同 **7a** 的 `data` 数组（仅该活动）。

### 8. 核销时长（管理员）

**接口**：`POST /activity/confirmHours/{registrationId}`

**需要管理员权限**

**请求示例**：
```bash
curl -X POST http://localhost:9000/activity/confirmHours/1 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "时长核销成功",
  "data": null
}
```

**说明**：
- 核销成功后，`vol_registration.hours_confirmed` 设为 `1`（已核销）
- 同时经 Feign 调用 `user-service` 更新用户的累计志愿时长

## 🧪 测试场景

### 场景1：完整的志愿活动流程

#### 步骤1：管理员登录
```bash
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

保存返回的 `token`。

#### 步骤2：创建活动
```bash
curl -X POST http://localhost:9000/activity/create \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "测试活动",
    "description": "这是一个测试活动",
    "location": "测试地点",
    "maxParticipants": 5,
    "volunteerHours": 2.0,
    "startTime": "2024-12-01T10:00:00",
    "endTime": "2024-12-01T12:00:00",
    "registrationDeadline": "2024-11-30T23:59:59",
    "category": "其他"
  }'
```

#### 步骤3：志愿者登录
```bash
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student01","password":"password123"}'
```

#### 步骤4：查看活动列表
```bash
curl http://localhost:9000/activity/list
```

#### 步骤5：报名活动
```bash
curl -X POST http://localhost:9000/activity/register/1 \
  -H "Authorization: Bearer STUDENT_TOKEN"
```

#### 步骤6：查看我的报名
```bash
curl http://localhost:9000/activity/myRegistrations \
  -H "Authorization: Bearer STUDENT_TOKEN"
```

#### 步骤7：管理员查看报名列表
```bash
curl http://localhost:9000/activity/admin/registrations \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

从返回的 `data` 中取待核销记录的 `id`，用于下一步。

#### 步骤8：管理员核销时长
```bash
curl -X POST http://localhost:9000/activity/confirmHours/1 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

（将 `1` 替换为步骤 7 中某条记录的 `id`。）

#### 步骤9：志愿者查看更新后的时长
```bash
curl http://localhost:9000/user/info \
  -H "Authorization: Bearer STUDENT_TOKEN"
```

### 场景2：防超卖测试

创建一个 `maxParticipants=5` 的活动，然后模拟 10 个用户同时报名：

```bash
# 使用脚本或 JMeter 并发请求
for i in {1..10}; do
  curl -X POST http://localhost:9000/activity/register/1 \
    -H "Authorization: Bearer TOKEN_$i" &
done
wait
```

预期结果：
- 前5个请求成功
- 后5个请求返回"名额已满"

### 场景3：JWT过期测试

1. 登录获取 Token
2. 等待 Token 过期（默认24小时，可修改配置缩短时间测试）
3. 使用过期 Token 请求接口
4. 预期返回 `401 Unauthorized`

## 🔍 调试工具

### 使用 Postman

1. 导入 API 集合
2. 设置环境变量：
   - `baseUrl`: `http://localhost:9000`
   - `token`: 登录后的 Token
3. 在 Headers 中自动添加：
   ```
   Authorization: Bearer {{token}}
   ```

### 使用 cURL

```bash
# 保存 Token 到变量（Linux/Mac）
export TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 使用变量
curl http://localhost:9000/user/info \
  -H "Authorization: Bearer $TOKEN"
```

### 使用浏览器开发者工具

1. 打开前端页面
2. F12 打开开发者工具
3. Network 标签查看请求
4. 复制为 cURL 命令

## 📝 常见错误

### 401 Unauthorized
- **原因**：Token 缺失、无效或过期
- **解决**：重新登录获取新 Token

### 403 Forbidden
- **原因**：权限不足（如普通用户访问管理员接口）
- **解决**：使用具有相应权限的账号

### 503 Service Unavailable
- **原因**：下游服务未启动或未注册到 Nacos
- **解决**：检查服务启动状态和 Nacos 注册

### 500 Internal Server Error
- **原因**：服务器内部错误
- **解决**：查看后端服务日志

## 🎯 性能测试

### 使用 Apache Bench

```bash
# 测试登录接口 QPS
ab -n 1000 -c 10 -p login.json -T application/json \
  http://localhost:9000/user/login
```

### 使用 JMeter

1. 创建线程组
2. 添加 HTTP 请求
3. 配置并发数和循环次数
4. 查看聚合报告

## 📊 监控端点

### Spring Boot Actuator

访问 http://localhost:9100 查看监控中心（Spring Boot Admin）

可查看：
- 服务健康状态
- JVM 内存使用
- HTTP 请求追踪
- 日志信息

---

💡 **提示**：建议使用 Postman 或类似工具保存常用的 API 请求，方便快速测试。
