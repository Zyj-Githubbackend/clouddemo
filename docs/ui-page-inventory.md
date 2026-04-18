# UI 页面清单（代码基准）

- 范围说明：只依据当前项目代码梳理页面、路由、菜单、按钮入口、弹窗/抽屉和调用接口；不采信 `README`、`docs`、注释中的理想描述。
- 统计口径：`frontend2/src/router/index.js` 中实际注册的业务页面共 18 个；另有 1 个布局壳路由 `/admin`；另有 1 个未接入页面文件 `frontend2/src/views/Home.vue`。
- 全局判定依据：路由以 `frontend2/src/router/index.js:3-163` 为准；普通端菜单以 `frontend2/src/components/Layout.vue:13-74` 为准；管理端菜单以 `frontend2/src/views/admin/AdminLayout.vue:10-65` 为准；请求/401 处理以 `frontend2/src/utils/request.js:5-47` 为准；登录态与管理员判断以 `frontend2/src/store/user.js:4-36` 为准；网关白名单和 `X-User-*` 注入以 `services/gateway-service/src/main/java/org/example/filter/AuthFilter.java:28-38`、`services/gateway-service/src/main/java/org/example/filter/AuthFilter.java:81-84` 为准。
- 关键结论：`/home` 实际加载的是 `frontend2/src/views/AnnouncementHome.vue`，不是 `frontend2/src/views/Home.vue`；`frontend2/src/views/Home.vue` 当前未接入任何路由。

## 普通端页面

# 登录页
- 路由路径：`/login`
- 所属模块：认证
- 入口位置：浏览器直达；注册页“去登录”；普通端/管理端登出后跳转
- 对应组件/文件：`Login`，`frontend2/src/views/Login.vue`
- 目标用户/角色：未登录用户
- 页面业务目标：提交账号密码获取 `token` 和 `userInfo`，并按角色跳转用户首页或管理后台
- 页面主要区域：品牌区；登录表单；注册按钮；快捷登录标签区
- 表单项：`username`、`password`
- 表格/列表字段：无；快捷登录标签为硬编码账号 `admin`、`student01`、`student02`
- 主要按钮与操作：`登录` 触发 `handleLogin`；`注册新账号` 跳 `/register`；点击快捷标签会填充账号并直接提交登录
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：提交时 `loading`；表单校验失败阻止提交；成功后管理员跳 `/admin/activities`、其他用户跳 `/home`；已登录访问 `/login` 会被路由守卫重定向到 `/home`；接口失败只显示全局 `ElMessage`
- 调用接口：`POST /api/user/login`；请求字段 `username`、`password`；返回字段 `token`、`userInfo`，其中 `userInfo` 结构见 `UserInfo`
- 依赖数据字典/枚举：角色仅分支判断 `ADMIN`
- 权限控制：页面自身不要求登录；`frontend2/src/router/index.js:150-153` 阻止已登录用户再次进入；登录成功后 token 与用户信息写入 `localStorage` 和 Pinia
- 当前完成度评估：高，登录主流程已完整接通
- 发现的问题：快捷登录账号写死在前端代码中；页面没有独立错误态，失败反馈完全依赖全局 toast
- 证据文件：`frontend2/src/router/index.js:9-18`；`frontend2/src/router/index.js:150-153`；`frontend2/src/views/Login.vue:17-142`；`frontend2/src/api/user.js:4-9`；`frontend2/src/store/user.js:11-18`；`services/user-service/src/main/java/org/example/controller/UserController.java:27-31`；`services/user-service/src/main/java/org/example/vo/LoginResponse.java:12-16`

# 注册页
- 路由路径：`/register`
- 所属模块：认证
- 入口位置：登录页“注册新账号”；浏览器直达
- 对应组件/文件：`Register`，`frontend2/src/views/Register.vue`
- 目标用户/角色：未登录用户；注册成功后的后台默认角色为 `VOLUNTEER`
- 页面业务目标：创建志愿者账号并跳回登录页
- 页面主要区域：左侧说明区；右侧注册表单
- 表单项：`username`、`realName`、`password`、`confirmPassword`、`studentNo`、`phone`、`email`
- 表格/列表字段：无
- 主要按钮与操作：`立即注册` 提交；“去登录”跳 `/login`
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：提交时 `loading`；前端校验包含确认密码、密码长度、手机号正则、邮箱格式；已登录访问 `/register` 会被路由守卫重定向到 `/home`
- 调用接口：`POST /api/user/register`；前端实际提交字段为 `username`、`password`、`realName`、`studentNo`、`phone`、`email`，`confirmPassword` 仅前端使用；后端注册 DTO 与前端提交字段一致
- 依赖数据字典/枚举：无
- 权限控制：页面自身不要求登录；后端 `UserService` 注册时写死新用户角色为 `VOLUNTEER`
- 当前完成度评估：高，注册闭环完整
- 发现的问题：没有注册成功后的自动登录；错误反馈仍只依赖全局 toast
- 证据文件：`frontend2/src/router/index.js:15-18`；`frontend2/src/router/index.js:150-153`；`frontend2/src/views/Register.vue:19-150`；`frontend2/src/api/user.js:13-18`；`services/user-service/src/main/java/org/example/controller/UserController.java:33-36`；`services/user-service/src/main/java/org/example/dto/RegisterRequest.java:8-20`；`services/user-service/src/main/java/org/example/service/UserService.java:77-103`

# 公告首页
- 路由路径：`/home`
- 所属模块：公告/首页
- 入口位置：根路由 `/` 重定向；普通端顶部菜单“首页”；管理端菜单“返回首页”；登录成功后普通用户默认跳转
- 对应组件/文件：`AnnouncementHome`，`frontend2/src/views/AnnouncementHome.vue`
- 目标用户/角色：已登录用户；代码未按角色区分管理员/志愿者
- 页面业务目标：展示最新公告，并把用户导向公告详情或活动列表
- 页面主要区域：首页 Hero；最新公告卡片网格
- 表单项：无
- 表格/列表字段：公告卡片使用 `id`、`imageUrl`、`title`、`content`、`publishTime`、`updateTime`、`activities`、`activityId`
- 主要按钮与操作：Hero 按钮“查看志愿活动”跳 `/activities`；卡片整体和“查看公告”跳 `/announcement/:id`；关联活动按钮跳 `/activity/:id`
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：页面根容器 `v-loading`；公告为空时显示 `ElEmpty`；没有独立错误页；无权限时前端会先被路由守卫拦到 `/login`
- 调用接口：`GET /api/announcement/home?limit=12`；返回 `AnnouncementVO` 列表，前端实际使用 `id`、`title`、`content`、`imageUrl`、`publishTime`、`updateTime`、`activities`、`activityId`
- 依赖数据字典/枚举：展示层把所有卡片状态写死为“已发布”，未读取可配置枚举
- 权限控制：前端路由 `meta.requireAuth=true`；但网关将 `/api/announcement/home` 放入白名单，所以“接口可匿名访问、页面不可匿名访问”以页面路由为准
- 当前完成度评估：中高，首页公告流可用
- 发现的问题：只展示首页公告，不承接全量公告列表；`frontend2/src/api/announcement.js` 已有 `getAnnouncementList`，但当前无页面/路由使用
- 证据文件：`frontend2/src/router/index.js:4-24`；`frontend2/src/components/Layout.vue:21-25`；`frontend2/src/views/admin/AdminLayout.vue:18`；`frontend2/src/views/AnnouncementHome.vue:3-111`；`frontend2/src/api/announcement.js:3-9`；`services/announcement-service/src/main/java/org/example/controller/AnnouncementController.java:48-51`；`services/announcement-service/src/main/java/org/example/vo/AnnouncementVO.java:12-44`；`services/gateway-service/src/main/java/org/example/filter/AuthFilter.java:33-36`

# 公告详情页
- 路由路径：`/announcement/:id`
- 所属模块：公告
- 入口位置：公告首页卡片；管理员公告管理页“查看”
- 对应组件/文件：`AnnouncementDetail`，`frontend2/src/views/AnnouncementDetail.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：展示单条公告正文、配图、关联活动和附件
- 页面主要区域：头图轮播区；公告内容卡；关联活动卡；附件卡；返回操作卡
- 表单项：无
- 表格/列表字段：正文区展示 `title`、`publishTime/updateTime`、`content`；关联活动区展示 `activities[].id/title/location/startTime` 或 `activityId` 回退；附件区展示 `attachments[].fileName/fileSize/url`
- 主要按钮与操作：关联活动按钮跳 `/activity/:id`；“返回公告首页”跳 `/home`；附件点击 `<a>` 直接访问附件 URL
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：页面根容器 `v-loading`；当 `announcement.id` 不存在且非加载中时显示“公告不存在或已下线”；无单独错误态
- 调用接口：`GET /api/announcement/{id}`；返回 `AnnouncementVO`，前端使用 `imageUrls/imageUrl`、`title`、`content`、`publishTime`、`updateTime`、`activities`、`activityId`、`attachments`
- 依赖数据字典/枚举：无独立枚举；附件大小用组件内 `formatFileSize`
- 权限控制：前端路由要求登录；后端详情接口只返回已发布公告，非已发布记录不会在该接口暴露
- 当前完成度评估：高，详情页主流程完整
- 发现的问题：附件下载没有前端兜底错误态；如果后端只返回 `activityId` 而不带 `activities`，前端只能显示“活动 #id”占位标题
- 证据文件：`frontend2/src/router/index.js:33-36`；`frontend2/src/views/AnnouncementDetail.vue:3-149`；`frontend2/src/api/announcement.js:22-27`；`services/announcement-service/src/main/java/org/example/controller/AnnouncementController.java:61-63`；`services/announcement-service/src/main/java/org/example/service/AnnouncementService.java:93-107`；`services/announcement-service/src/main/java/org/example/vo/AnnouncementVO.java:12-44`；`services/announcement-service/src/main/java/org/example/vo/AnnouncementAttachmentVO.java:8-18`

# 活动列表页
- 路由路径：`/activities`
- 所属模块：活动
- 入口位置：普通端顶部菜单“活动”；公告首页 Hero 按钮；未接入 `Home.vue` 的“查看活动”按钮
- 对应组件/文件：`ActivityList`，`frontend2/src/views/ActivityList.vue`
- 目标用户/角色：已登录用户；代码未按角色区分管理员/志愿者
- 页面业务目标：按状态、招募阶段、活动类型筛选活动并进入详情
- 页面主要区域：页面横幅；筛选卡；活动卡片网格；分页条
- 表单项：筛选项 `status`、`recruitmentPhase`、`category`
- 表格/列表字段：活动卡片展示 `imageUrl`、`title`、`category`、`location`、`registrationDeadline`、`volunteerHours`、`currentParticipants`、`maxParticipants`
- 主要按钮与操作：点击筛选 pill 会切换筛选并重新查询；点击活动卡片跳 `/activity/:id`；分页切换页码/每页条数会重新查询
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：卡片区 `v-loading`；无数据时显示“暂无符合条件的活动”；无独立错误页
- 调用接口：`GET /api/activity/list`；请求参数 `page`、`size`、`status`、`category`、`recruitmentPhase`；响应分页字段 `records`、`total`，记录结构使用 `ActivityVO`
- 依赖数据字典/枚举：活动状态筛选 `RECRUITING/COMPLETED/CANCELLED`；招募阶段筛选 `NOT_STARTED/RECRUITING/ENDED`；活动分类固定 6 个中文值；展示状态依赖 `frontend2/src/utils/recruitment.js` 和 `frontend2/src/utils/activityPhase.js`
- 权限控制：前端路由要求登录；但网关对白名单开放了 `/api/activity/list`，因此代码实际是“接口可匿名、页面不可匿名”
- 当前完成度评估：高，筛选、列表、分页完整
- 发现的问题：没有关键字、时间、地点等更细筛选；`formatDate` 没有空值保护，后端若返回空时间会出现 `Invalid Date`
- 证据文件：`frontend2/src/router/index.js:27-30`；`frontend2/src/components/Layout.vue:22`；`frontend2/src/views/ActivityList.vue:10-180`；`frontend2/src/api/activity.js:4-18`；`frontend2/src/utils/recruitment.js:7-20`；`frontend2/src/utils/activityPhase.js:7-30`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:47-58`；`services/activity-service/src/main/java/org/example/vo/ActivityVO.java:11-49`

# 活动详情页
- 路由路径：`/activity/:id`
- 所属模块：活动
- 入口位置：活动列表卡片；公告详情关联活动；管理员活动管理“查看”；未接入 `Home.vue` 活动卡片
- 对应组件/文件：`ActivityDetail`，`frontend2/src/views/ActivityDetail.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：展示活动详情、报名进度，并提供报名/取消报名入口
- 页面主要区域：Hero 头图与标签；活动说明；时间安排；报名进度卡；报名动作卡；志愿时长卡
- 表单项：无
- 表格/列表字段：展示 `category`、`title`、`location`、`volunteerHours`、`currentParticipants`、`maxParticipants`、`imageUrls/imageUrl`、`description`、`registrationStartTime`、`registrationDeadline`、`startTime`、`endTime`、`availableSlots`、`isRegistered`、`status`
- 主要按钮与操作：`立即报名` 调用报名接口；已报名且允许取消时显示 `取消报名`；取消报名会二次确认
- 弹窗/抽屉/二级页面：`ElMessageBox.confirm` 取消报名确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：页面根容器 `v-loading`；活动不存在时显示“活动不存在或已删除”；报名动作区有 `招募尚未开始/招募已结束/活动已取消/活动已结束/活动暂不可报名` 状态文案
- 调用接口：`GET /api/activity/{id}` 获取详情；`POST /api/activity/register/{activityId}` 报名；`POST /api/activity/cancelRegistration/{activityId}` 取消报名；响应主体为 `ActivityVO`
- 依赖数据字典/枚举：活动阶段依赖 `getActivityPhaseDisplay`；招募阶段依赖 `getRecruitmentDisplay`；活动状态使用 `RECRUITING/COMPLETED/CANCELLED`
- 权限控制：前端路由要求登录；后端报名逻辑还会校验活动状态、报名窗口、重复报名、余量等，最终以后端规则为准
- 当前完成度评估：中高，详情和操作可用
- 发现的问题：前端 `canRegister` 只根据 `status==='RECRUITING'` 和 `getRecruitmentDisplay(activity).text==='招募中'` 判断，可用性条件比后端更宽；页面没有独立错误态，失败反馈只靠全局 toast
- 证据文件：`frontend2/src/router/index.js:39-42`；`frontend2/src/views/ActivityDetail.vue:3-237`；`frontend2/src/api/activity.js:20-79`；`frontend2/src/utils/recruitment.js:7-20`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:105-120`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:273-280`；`services/activity-service/src/main/java/org/example/vo/ActivityVO.java:11-49`；`services/activity-service/src/main/java/org/example/service/ActivityService.java:149-166`；`services/activity-service/src/main/java/org/example/service/ActivityService.java:328-411`

# 提交反馈页
- 路由路径：`/feedback`
- 所属模块：反馈
- 入口位置：我的反馈页“提交反馈”按钮；浏览器直达
- 对应组件/文件：`FeedbackCreate`，`frontend2/src/views/FeedbackCreate.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：创建新的反馈工单，并允许首条消息携带附件
- 页面主要区域：页面头部；反馈表单；底部操作区
- 表单项：`title`、`category`、`content`、`attachments`
- 表格/列表字段：无；附件列表由 `FeedbackAttachmentUploader` 渲染，显示 `fileName`、`fileSize`、`contentType`、`fileType`
- 主要按钮与操作：`我的反馈` 跳 `/my-feedback`；`返回` 执行 `router.back()`；`提交反馈` 调用创建接口并在成功后跳到详情页
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：提交时 `submitting`；校验要求标题、分类，以及“内容或附件至少有一项”；无单独错误页
- 调用接口：`POST /api/feedback`；请求字段 `title`、`category`、`content`、`attachments`；创建成功后前端读取 `res.data.id` 跳 `/feedback/{id}`；附件上传通过 `POST /api/feedback/attachments`
- 依赖数据字典/枚举：分类固定为 `QUESTION/SUGGESTION/BUG/COMPLAINT/OTHER`；附件上传组件限制最多 6 个附件，接受图片、`pdf/xls/xlsx/doc/docx/txt/csv`
- 权限控制：前端路由要求登录；后端创建和上传附件都要求 `X-User-Id`
- 当前完成度评估：高，创建闭环完整
- 发现的问题：没有草稿、预览或保存后继续编辑能力；错误态仍完全依赖全局 toast
- 证据文件：`frontend2/src/router/index.js:45-48`；`frontend2/src/views/FeedbackCreate.vue:13-111`；`frontend2/src/components/FeedbackAttachmentUploader.vue:20-110`；`frontend2/src/api/feedback.js:4-58`；`services/feedback-service/src/main/java/org/example/controller/FeedbackController.java:48-53`；`services/feedback-service/src/main/java/org/example/dto/FeedbackCreateRequest.java:8-16`；`services/feedback-service/src/main/java/org/example/vo/FeedbackAttachmentUploadVO.java:12-24`

# 我的反馈页
- 路由路径：`/my-feedback`
- 所属模块：反馈
- 入口位置：普通端顶部菜单“反馈”；提交反馈页头部“我的反馈”
- 对应组件/文件：`MyFeedback`，`frontend2/src/views/MyFeedback.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：查看本人反馈工单列表，并进入详情继续沟通
- 页面主要区域：页头；状态筛选工具栏；反馈表格；分页条
- 表单项：筛选项 `status`
- 表格/列表字段：`title`、`category`、`status`、`priority`、`lastMessageTime/updateTime`
- 主要按钮与操作：`提交反馈` 跳 `/feedback`；点击表格行或“查看”跳 `/feedback/:id`；切换状态或分页重新查询
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；无数据时显示“暂无反馈记录”；无独立错误页
- 调用接口：`GET /api/feedback/my`；请求参数 `page`、`size`、`status`；响应分页字段 `records`、`total`，记录结构为 `FeedbackVO`
- 依赖数据字典/枚举：状态映射 `OPEN/REPLIED/CLOSED/REJECTED`；分类映射 `QUESTION/SUGGESTION/BUG/COMPLAINT/OTHER`；优先级映射 `LOW/NORMAL/HIGH/URGENT`
- 权限控制：前端路由要求登录；后端列表接口用 `X-User-Id` 只返回当前用户数据
- 当前完成度评估：高，列表与跳转完整
- 发现的问题：只有状态筛选，没有分类/优先级/关键词筛选；页面没有独立错误态
- 证据文件：`frontend2/src/router/index.js:51-54`；`frontend2/src/components/Layout.vue:23`；`frontend2/src/views/MyFeedback.vue:13-132`；`frontend2/src/api/feedback.js:12-23`；`services/feedback-service/src/main/java/org/example/controller/FeedbackController.java:55-62`；`services/feedback-service/src/main/java/org/example/vo/FeedbackVO.java:9-35`

# 反馈详情页
- 路由路径：`/feedback/:id`
- 所属模块：反馈
- 入口位置：我的反馈列表行点击/“查看”；提交反馈成功后自动跳转
- 对应组件/文件：`FeedbackDetail`，`frontend2/src/views/FeedbackDetail.vue`
- 目标用户/角色：默认入口面向反馈提交人；管理员是否应直接复用该路由页面，代码层无法从菜单入口确认，标记为待确认
- 页面业务目标：查看单条反馈的消息时间线，继续补充信息，或在问题解决后关闭反馈
- 页面主要区域：页头元信息；消息时间线；回复卡；终态提示；侧边处理信息卡
- 表单项：回复区 `content`、`attachments`
- 表格/列表字段：消息区展示 `messages[].senderRole/createTime/content/attachments`；侧边信息展示 `status`、`lastReplierRole`、`lastMessageTime/updateTime`、`rejectReason`
- 主要按钮与操作：返回“我的反馈”；`发送消息` 调用户回复接口；`确认关闭` 调关闭接口；附件列表可预览图片和下载文件
- 弹窗/抽屉/二级页面：关闭反馈确认框；附件图片预览 `ElImageViewer`
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：消息区 `v-loading`；`status` 为 `OPEN/REPLIED` 才允许回复与关闭；`CLOSED/REJECTED` 时显示终态 `ElAlert`；页面没有“工单不存在”专门空态
- 调用接口：`GET /api/feedback/{id}` 获取详情；`POST /api/feedback/{id}/messages` 追加消息；`POST /api/feedback/{id}/close` 关闭反馈；附件下载走 `GET /api/feedback/attachments`
- 依赖数据字典/枚举：状态 `OPEN/REPLIED/CLOSED/REJECTED`；分类 `QUESTION/SUGGESTION/BUG/COMPLAINT/OTHER`；优先级 `LOW/NORMAL/HIGH/URGENT`；角色 `VOLUNTEER/ADMIN/SYSTEM`
- 权限控制：前端路由只要求登录；后端 `feedbackService.getFeedbackDetail(id,userId,role)` 会校验“管理员或工单所有者”才能查看；用户回复/关闭接口只接受当前用户
- 当前完成度评估：高，查看、回复、关闭、附件下载均已接通
- 发现的问题：没有“详情不存在/无权限”的独立占位页；管理员虽然可能具备后端接口权限，但前端无专门入口复用该页面，产品意图待确认
- 证据文件：`frontend2/src/router/index.js:57-60`；`frontend2/src/views/FeedbackDetail.vue:3-195`；`frontend2/src/components/FeedbackAttachmentList.vue:1-92`；`frontend2/src/components/FeedbackAttachmentUploader.vue:20-110`；`frontend2/src/api/feedback.js:25-135`；`services/feedback-service/src/main/java/org/example/controller/FeedbackController.java:139-160`；`services/feedback-service/src/main/java/org/example/vo/FeedbackDetailVO.java:8-12`；`services/feedback-service/src/main/java/org/example/vo/FeedbackMessageVO.java:10-26`；`services/feedback-service/src/main/java/org/example/service/FeedbackService.java:152-169`；`services/feedback-service/src/main/java/org/example/service/FeedbackService.java:237-253`

# 我的志愿足迹页
- 路由路径：`/my`
- 所属模块：个人中心/活动报名
- 入口位置：普通端顶部菜单“我的志愿足迹”；头像下拉“我的报名”
- 对应组件/文件：`MyCenter`，`frontend2/src/views/MyCenter.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：查看个人报名记录、取消未开始活动的报名，并导出已核销记录
- 页面主要区域：Hero 统计区；摘要卡；表格工具栏；报名记录表格
- 表单项：工具栏开关 `showConfirmedOnly`
- 表格/列表字段：`activityTitle`、`volunteerHours`、`startTime`、`registrationTime`、`confirmTime`、`checkInStatus`、`hoursConfirmed`、`status`
- 主要按钮与操作：`导出已核销记录` 下载 Excel；`取消报名` 二次确认后调用取消接口；开关可切换“仅看已核销/显示全部”
- 弹窗/抽屉/二级页面：取消报名确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；全量为空时显示“还没有报名记录”；仅看已核销为空时显示“暂无已核销记录”；导出时 `exporting`
- 调用接口：`GET /api/activity/myRegistrations` 返回 `RegistrationVO[]`；`POST /api/activity/cancelRegistration/{activityId}` 取消报名；`GET /api/activity/myRegistrations/exportConfirmed` 下载已核销记录 Excel
- 依赖数据字典/枚举：签到状态 `checkInStatus===1` 视为已签到；核销状态 `hoursConfirmed===1` 视为已核销；报名状态 `REGISTERED/CANCELLED`
- 权限控制：前端路由要求登录；后端所有接口都以 `X-User-Id` 绑定当前用户；取消报名最终以后端规则为准
- 当前完成度评估：高，列表、导出、取消报名均已接通
- 发现的问题：没有分页，记录很多时会一次性加载；列表内没有进入活动详情的快捷入口
- 证据文件：`frontend2/src/router/index.js:63-66`；`frontend2/src/components/Layout.vue:24`；`frontend2/src/components/Layout.vue:37-38`；`frontend2/src/views/MyCenter.vue:3-265`；`frontend2/src/api/activity.js:74-97`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:114-147`；`services/activity-service/src/main/java/org/example/vo/RegistrationVO.java:10-43`；`services/activity-service/src/main/java/org/example/service/ActivityService.java:374-418`

# 个人资料页
- 路由路径：`/profile`
- 所属模块：个人中心
- 入口位置：头像下拉“个人资料”
- 对应组件/文件：`Profile`，`frontend2/src/views/Profile.vue`
- 目标用户/角色：已登录用户
- 页面业务目标：查看和修改个人资料、修改密码
- 页面主要区域：个人卡片；统计卡片；功能说明卡；编辑资料弹窗；修改密码弹窗
- 表单项：编辑资料弹窗 `realName`、`phone`、`email`；修改密码弹窗 `oldPassword`、`newPassword`、`confirmPassword`
- 表格/列表字段：展示字段 `realName`、`studentNo`、`role`、`username`、`phone`、`email`、`totalVolunteerHours`；统计卡展示 `registrationCount`、`checkedInCount`、`confirmedCount`
- 主要按钮与操作：`编辑资料` 打开编辑弹窗；`修改密码` 打开密码弹窗；各弹窗“确定”分别调用更新接口
- 弹窗/抽屉/二级页面：编辑资料 `ElDialog`；修改密码 `ElDialog`
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：页面根容器 `v-loading`；弹窗提交共用 `updateLoading`；密码修改成功后清空本地登录态并 1 秒后跳 `/login`
- 调用接口：`GET /api/user/info` 获取资料；`PUT /api/user/update` 更新资料；`PUT /api/user/updatePassword` 修改密码，提交字段只包含 `oldPassword`、`newPassword`、`confirmPassword`
- 依赖数据字典/枚举：角色展示只区分 `ADMIN` 和非 `ADMIN`
- 权限控制：前端路由要求登录；后端 `get/update/updatePassword` 都依赖 `X-User-Id`
- 当前完成度评估：中，资料查看和编辑可用，但统计能力不完整
- 发现的问题：`registrationCount`、`checkedInCount`、`confirmedCount` 被硬编码为 `0`；更新资料后只修改当前页面本地 `userInfo`，没有同步 `Pinia/localStorage`，顶部导航中的姓名可能不会立即刷新
- 证据文件：`frontend2/src/router/index.js:69-72`；`frontend2/src/components/Layout.vue:36-38`；`frontend2/src/views/Profile.vue:3-269`；`frontend2/src/api/user.js:21-44`；`frontend2/src/store/user.js:4-36`；`services/user-service/src/main/java/org/example/controller/UserController.java:39-60`；`services/user-service/src/main/java/org/example/vo/UserInfo.java:9-25`

## 管理端页面

# 管理后台壳路由
- 路由路径：`/admin`
- 所属模块：管理端布局
- 入口位置：普通端顶部菜单 `userStore.isAdmin` 条件下显示“管理后台”；浏览器直达 `/admin/...`
- 对应组件/文件：`AdminLayout`，`frontend2/src/views/admin/AdminLayout.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：提供管理端侧边菜单、顶部栏和子路由容器
- 页面主要区域：侧边菜单；顶部栏；移动端抽屉菜单；`router-view`
- 表单项：无
- 表格/列表字段：无
- 主要按钮与操作：侧边菜单/抽屉切换子页面；`返回首页` 跳 `/home`；头像下拉 `logout`
- 弹窗/抽屉/二级页面：移动端菜单 `ElDrawer`；登出确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：无页面级加载；非管理员访问时路由守卫直接跳 `/home`；直接访问 `/admin` 本身没有默认重定向子路由
- 调用接口：无页面级接口
- 依赖数据字典/枚举：无
- 权限控制：父路由 `meta.requireAuth=true` 且 `meta.requireAdmin=true`；后端各管理接口还会再校验 `X-User-Role==='ADMIN'`
- 当前完成度评估：中，布局壳可用
- 发现的问题：`/admin` 没有默认子路由或重定向，直接进入该路径时 `router-view` 会是空白容器
- 证据文件：`frontend2/src/router/index.js:75-122`；`frontend2/src/router/index.js:139-147`；`frontend2/src/components/Layout.vue:25`；`frontend2/src/components/Layout.vue:68`；`frontend2/src/views/admin/AdminLayout.vue:2-114`

# 公告管理页
- 路由路径：`/admin/announcements`
- 所属模块：管理端/公告
- 入口位置：管理端侧边菜单“公告管理”
- 对应组件/文件：`AnnouncementManage`，`frontend2/src/views/admin/AnnouncementManage.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：管理首页公告，支持创建、编辑、发布、下线、删除，并绑定活动与附件
- 页面主要区域：页头；状态筛选工具栏；公告表格；分页条；新建/编辑公告弹窗
- 表单项：弹窗表单包含 `title`、`activityIds`、`status`、`sortOrder`、`content`、`imageKeys/imageUrls`、`attachments`
- 表格/列表字段：`title`、`status`、`activities/activityId`、`attachments.length`、`sortOrder`、`publishTime/updateTime`
- 主要按钮与操作：`发布公告` 打开新建弹窗；表格中 `查看`、`编辑`、`发布/下线`、`删除`；状态筛选和分页会重新查询
- 弹窗/抽屉/二级页面：新建/编辑公告 `ElDialog`
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；弹窗提交时 `submitLoading`；没有单独 `ElEmpty`；非管理员会被前端路由和后端 controller 双重拦截
- 调用接口：`GET /api/announcement/admin/list`；`GET /api/announcement/admin/{id}`；`POST /api/announcement/admin`；`PUT /api/announcement/admin/{id}`；`POST /api/announcement/admin/{id}/publish`；`POST /api/announcement/admin/{id}/offline`；`DELETE /api/announcement/admin/{id}`；活动选项来自 `GET /api/activity/list?page=1&size=100`；图片上传 `POST /api/announcement/admin/image`；附件上传 `POST /api/announcement/admin/attachment`
- 依赖数据字典/枚举：公告状态 `PUBLISHED/OFFLINE`；附件上传限制最多 8 个，仅接受 `pdf/xls/xlsx/doc/docx/txt/csv`
- 权限控制：父路由要求管理员；后端公告管理 controller 对 `/admin/*` 全部显式校验 `role==='ADMIN'`
- 当前完成度评估：高，列表、编辑、上下线、删除闭环完整
- 发现的问题：提交载荷同时带 `activityId` 和 `activityIds`；关联活动选项只拉取前 100 条公共活动列表，没有专门的管理端活动选择接口
- 证据文件：`frontend2/src/router/index.js:80-85`；`frontend2/src/views/admin/AdminLayout.vue:11`；`frontend2/src/views/admin/AnnouncementManage.vue:14-386`；`frontend2/src/api/announcement.js:29-109`；`frontend2/src/api/activity.js:4-18`；`frontend2/src/components/AnnouncementImageUploader.vue:23-138`；`frontend2/src/components/AnnouncementAttachmentUploader.vue:17-132`；`services/announcement-service/src/main/java/org/example/controller/AnnouncementController.java:66-175`；`services/announcement-service/src/main/java/org/example/dto/AnnouncementRequest.java:8-26`

# 反馈工单管理页
- 路由路径：`/admin/feedback`
- 所属模块：管理端/反馈
- 入口位置：管理端侧边菜单“反馈工单”
- 对应组件/文件：`FeedbackManage`，`frontend2/src/views/admin/FeedbackManage.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：查看全部反馈、筛选工单、回复、关闭、驳回、调整优先级
- 页面主要区域：页头；多条件筛选栏；反馈表格；分页条；详情抽屉；回复卡
- 表单项：筛选项 `status`、`category`、`priority`、`keyword`；抽屉内优先级修改 `priority`；回复表单 `content`、`attachments`；驳回原因使用 prompt 输入
- 表格/列表字段：`title`、`userId`、`category`、`status`、`priority`、`lastReplierRole`、`lastMessageTime/updateTime`
- 主要按钮与操作：`查看` 打开详情抽屉；`关闭`、`驳回` 处理当前工单；抽屉内可 `保存优先级`、`关闭`、`驳回`、`发送回复`
- 弹窗/抽屉/二级页面：详情 `ElDrawer`；驳回原因 `ElMessageBox.prompt`
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；抽屉内容 `detailLoading`；回复和优先级保存有独立 loading；没有单独空态组件
- 调用接口：`GET /api/feedback/admin/list`；`GET /api/feedback/admin/{id}`；`POST /api/feedback/admin/{id}/messages`；`POST /api/feedback/admin/{id}/close`；`POST /api/feedback/admin/{id}/reject`；`POST /api/feedback/admin/{id}/priority`；附件上传与下载复用普通端接口 `POST/GET /api/feedback/attachments`
- 依赖数据字典/枚举：状态 `OPEN/REPLIED/CLOSED/REJECTED`；分类 `QUESTION/SUGGESTION/BUG/COMPLAINT/OTHER`；优先级 `LOW/NORMAL/HIGH/URGENT`；角色 `VOLUNTEER/ADMIN/SYSTEM`
- 权限控制：父路由要求管理员；后端 `/feedback/admin/*` 全部校验 `FeedbackService.ROLE_ADMIN`
- 当前完成度评估：高，管理闭环完整
- 发现的问题：列表只展示 `userId`，没有拉取用户名/姓名辅助识别；没有单独空态组件
- 证据文件：`frontend2/src/router/index.js:86-91`；`frontend2/src/views/admin/AdminLayout.vue:12`；`frontend2/src/views/admin/FeedbackManage.vue:13-332`；`frontend2/src/api/feedback.js:60-135`；`frontend2/src/components/FeedbackAttachmentUploader.vue:20-110`；`frontend2/src/components/FeedbackAttachmentList.vue:1-92`；`services/feedback-service/src/main/java/org/example/controller/FeedbackController.java:64-137`；`services/feedback-service/src/main/java/org/example/service/FeedbackService.java:146-237`

# 活动管理页
- 路由路径：`/admin/activities`
- 所属模块：管理端/活动
- 入口位置：普通端管理员菜单“管理后台”的默认落点；管理端侧边菜单“活动管理”
- 对应组件/文件：`ActivityManage`，`frontend2/src/views/admin/ActivityManage.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：查看活动列表，编辑可编辑活动，查看报名名单，取消/结项/删除活动
- 页面主要区域：页头；活动表格；分页条；报名名单弹窗；编辑活动弹窗
- 表单项：编辑弹窗包含 `title`、`category`、`location`、`editAiKeywords`、`description`、`imageKeys/imageUrls`、`maxParticipants`、`volunteerHours`、`startTime`、`endTime`、`registrationStartTime`、`registrationDeadline`
- 表格/列表字段：`title`、`category`、`location`、`currentParticipants/maxParticipants`、`startTime`、活动阶段标签、招募状态标签；报名名单弹窗字段为 `realName`、`studentNo`、`username`、`phone`、`registrationTime`、`checkInStatus`、`hoursConfirmed`
- 主要按钮与操作：表格中有 `查看`、`编辑`、`取消活动`、`结项`、`报名名单`、`删除`；编辑弹窗支持 `AI生成` 文案和保存；分页切换重新查询
- 弹窗/抽屉/二级页面：报名名单 `ElDialog`；编辑活动 `ElDialog`；取消活动/结项/删除均有确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；编辑详情加载 `editDetailLoading`；报名名单加载 `regLoading`；完成/取消的行通过 `rowClass` 改样式；没有独立空态组件
- 调用接口：列表使用 `GET /api/activity/list`；操作接口包括 `DELETE /api/activity/{id}`、`GET /api/activity/{id}/registrations`、`GET /api/activity/{id}`、`PUT /api/activity/{id}`、`POST /api/activity/ai/generate`、`POST /api/activity/{id}/cancel`、`POST /api/activity/{id}/complete`
- 依赖数据字典/枚举：活动分类固定 6 个中文值；活动阶段展示依赖 `getActivityPhaseDisplay`；招募状态展示依赖 `getRecruitmentDisplay`
- 权限控制：父路由要求管理员；后端编辑/删除/取消/结项/报名名单接口都校验 `role==='ADMIN'`
- 当前完成度评估：中高，核心管理动作已接通
- 发现的问题：没有任何搜索/筛选工具栏；管理列表复用了公共 `GET /activity/list`，并没有单独管理端列表接口；`frontend2/src/api/activity.js` 已有 `getAdminRegistrations`，但当前前端完全未使用；编辑弹窗与“发布活动页”表单高度重复
- 证据文件：`frontend2/src/router/index.js:92-97`；`frontend2/src/views/admin/AdminLayout.vue:13`；`frontend2/src/views/admin/ActivityManage.vue:11-533`；`frontend2/src/api/activity.js:4-18`；`frontend2/src/api/activity.js:28-52`；`frontend2/src/api/activity.js:107-120`；`frontend2/src/components/ActivityImageUploader.vue:23-138`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:74-103`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:154-213`

# 发布活动页
- 路由路径：`/admin/create`
- 所属模块：管理端/活动
- 入口位置：管理端侧边菜单“发布活动”
- 对应组件/文件：`ActivityCreate`，`frontend2/src/views/admin/ActivityCreate.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：创建新的志愿活动并立即上架到活动列表
- 页面主要区域：Hero；基础信息卡；活动介绍卡；图片上传卡；时间安排卡；底部操作条
- 表单项：`title`、`category`、`location`、`maxParticipants`、`volunteerHours`、`description`、`imageKeys/imageUrls`、`startTime`、`endTime`、`registrationStartTime`、`registrationDeadline`、`aiKeywords`
- 表格/列表字段：无
- 主要按钮与操作：`AI生成` 根据 `location/category/keywords/volunteerHours` 生成描述；`重置` 重置表单；`发布活动` 提交后跳 `/admin/activities`
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：提交使用 `loading`；AI 生成使用 `aiLoading`；前端校验了时间先后关系；无独立错误页
- 调用接口：`POST /api/activity/create`；`POST /api/activity/ai/generate`；图片上传 `POST /api/activity/admin/image`
- 依赖数据字典/枚举：活动分类固定 6 个中文值；时间校验要求 `registrationStartTime < registrationDeadline <= startTime < endTime`
- 权限控制：父路由要求管理员；后端创建、AI 生成、图片上传都校验 `role==='ADMIN'`
- 当前完成度评估：高，创建流程完整
- 发现的问题：与活动管理页编辑弹窗共用同一套业务字段，但代码重复维护两份；没有活动预览或草稿能力
- 证据文件：`frontend2/src/router/index.js:98-103`；`frontend2/src/views/admin/AdminLayout.vue:14`；`frontend2/src/views/admin/ActivityCreate.vue:9-343`；`frontend2/src/api/activity.js:35-42`；`frontend2/src/api/activity.js:145-165`；`frontend2/src/components/ActivityImageUploader.vue:23-138`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:60-71`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:228-255`；`services/activity-service/src/main/java/org/example/dto/ActivityCreateRequest.java:11-35`；`services/activity-service/src/main/java/org/example/dto/AIGenerateRequest.java:8-17`

# 活动签到页
- 路由路径：`/admin/checkin`
- 所属模块：管理端/活动执行
- 入口位置：管理端侧边菜单“活动签到”
- 对应组件/文件：`ActivityCheckIn`，`frontend2/src/views/admin/ActivityCheckIn.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：在活动进行中对报名记录逐条标记签到
- 页面主要区域：页头；活动选择器；当前活动摘要；报名记录表格
- 表单项：活动选择 `selectedActivityId`
- 表格/列表字段：`realName`、`studentNo`、`phone`、`registrationTime`、`checkInStatus`、`checkInTime`
- 主要按钮与操作：选择活动后拉取报名记录；点击 `标记签到` 进行签到；签到成功后刷新活动列表和报名记录
- 弹窗/抽屉/二级页面：签到确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：活动下拉加载 `activitiesLoading`；报名表格 `loading`；无可签到活动时显示空态；未选择活动时显示提示空态
- 调用接口：`GET /api/activity/admin/checkInActivities`；`GET /api/activity/{activityId}/registrations`；`POST /api/activity/admin/checkIn/{registrationId}`
- 依赖数据字典/枚举：签到状态按 `checkInStatus===1` 判断
- 权限控制：父路由要求管理员；后端接口全部校验 `role==='ADMIN'`，且仅返回“已开始且未结束”的活动
- 当前完成度评估：高，签到主流程完整
- 发现的问题：只支持逐条点击签到，没有批量或扫码能力；表格没有关键字筛选
- 证据文件：`frontend2/src/router/index.js:104-109`；`frontend2/src/views/admin/AdminLayout.vue:15`；`frontend2/src/views/admin/ActivityCheckIn.vue:11-155`；`frontend2/src/api/activity.js:115-143`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:179-199`；`services/activity-service/src/main/java/org/example/service/ActivityService.java:499-547`

# 时长核销页
- 路由路径：`/admin/confirm`
- 所属模块：管理端/活动执行
- 入口位置：管理端侧边菜单“时长核销”
- 对应组件/文件：`HoursConfirm`，`frontend2/src/views/admin/HoursConfirm.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：对已结束但未结项活动中的签到记录逐条核销志愿时长
- 页面主要区域：页头；活动选择器；当前活动摘要；报名记录表格
- 表单项：活动选择 `selectedActivityId`
- 表格/列表字段：`realName`、`studentNo`、`phone`、`volunteerHours`、`checkInStatus`、`hoursConfirmed`、`confirmTime`
- 主要按钮与操作：选择活动后拉取报名记录；对 `hoursConfirmed===0 && checkInStatus===1` 的记录显示 `核销` 按钮；核销成功后刷新活动列表和记录
- 弹窗/抽屉/二级页面：核销确认框
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：活动下拉加载 `endedLoading`；报名表格 `loading`；无可核销活动时显示空态；未选择活动时显示提示空态
- 调用接口：`GET /api/activity/admin/endedActivities`；`GET /api/activity/{activityId}/registrations`；`POST /api/activity/confirmHours/{registrationId}`
- 依赖数据字典/枚举：签到状态按 `checkInStatus===1` 判断；核销状态按 `hoursConfirmed===1` 判断
- 权限控制：父路由要求管理员；后端接口全部校验 `role==='ADMIN'`，并限制只能核销“已签到、未核销、活动已结束、活动未取消”的记录
- 当前完成度评估：高，核销主流程完整
- 发现的问题：只支持逐条核销，没有批量操作；表格没有搜索筛选
- 证据文件：`frontend2/src/router/index.js:110-115`；`frontend2/src/views/admin/AdminLayout.vue:16`；`frontend2/src/views/admin/HoursConfirm.vue:11-163`；`frontend2/src/api/activity.js:99-127`；`services/activity-service/src/main/java/org/example/controller/ActivityController.java:168-226`；`services/activity-service/src/main/java/org/example/service/ActivityService.java:484-590`

# 志愿时长查询页
- 路由路径：`/admin/hours`
- 所属模块：管理端/用户
- 入口位置：管理端侧边菜单“时长查询”
- 对应组件/文件：`VolunteerHours`，`frontend2/src/views/admin/VolunteerHours.vue`
- 目标用户/角色：`ADMIN`
- 页面业务目标：按姓名/学号/用户名搜索志愿者累计时长并查看汇总统计
- 页面主要区域：页头；关键词工具栏；时长表格；底部统计区
- 表单项：关键词 `keyword`
- 表格/列表字段：序号、`realName`、`studentNo`、`username`、`phone`、`email`、`totalVolunteerHours`
- 主要按钮与操作：输入关键词后点击 `查询` 或按回车重新拉取数据；清空关键词时自动重查
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：表格 `v-loading`；无数据时显示“未找到匹配的志愿者”；底部统计区在有数据时显示总人数/总时长/平均时长
- 调用接口：`GET /api/user/admin/hours`；请求参数 `keyword`；响应字段结构为 `UserInfo[]`
- 依赖数据字典/枚举：无；仅按时长值把样式分成 `>=20`、`>=8`、其余三档
- 权限控制：父路由要求管理员；后端 `/user/admin/hours` 再次显式校验 `role==='ADMIN'`
- 当前完成度评估：高，查询主流程完整
- 发现的问题：没有分页、导出或排序维度扩展；只支持一个关键词输入框
- 证据文件：`frontend2/src/router/index.js:116-121`；`frontend2/src/views/admin/AdminLayout.vue:17`；`frontend2/src/views/admin/VolunteerHours.vue:11-98`；`frontend2/src/api/user.js:47-53`；`services/user-service/src/main/java/org/example/controller/UserController.java:63-72`；`services/user-service/src/main/java/org/example/service/UserService.java:156-175`；`services/user-service/src/main/java/org/example/vo/UserInfo.java:9-25`

## 未接入页面

# 未接入首页候选页
- 路由路径：待确认；当前代码中没有任何路由指向该组件
- 所属模块：首页/活动概览
- 入口位置：无；当前菜单、路由和登录跳转均未引用
- 对应组件/文件：`Home`，`frontend2/src/views/Home.vue`
- 目标用户/角色：从代码用途看是已登录普通用户；但由于未接入，最终面向谁待确认
- 页面业务目标：展示个人志愿统计卡和最新活动列表
- 页面主要区域：Hero；统计卡；最新活动卡片
- 表单项：无
- 表格/列表字段：统计卡使用 `totalVolunteerHours`、报名数、签到数、核销数；活动卡使用 `imageUrl`、`title`、`category`、`location`、`volunteerHours`、`currentParticipants`、`maxParticipants`
- 主要按钮与操作：`查看活动` 跳 `/activities`；点击活动卡片跳 `/activity/:id`
- 弹窗/抽屉/二级页面：无
- 页面状态（加载中/空状态/无权限/错误态/成功态等）：没有专门 loading/empty 容器；仅在 `onMounted` 时并发取数
- 调用接口：`GET /api/activity/list?page=1&size=20&recruitmentPhase=RECRUITING`；`GET /api/activity/myRegistrations`；`GET /api/user/info`
- 依赖数据字典/枚举：活动卡状态标签依赖 `getRecruitmentDisplay`
- 权限控制：组件自身没有路由元信息，因为没有接入路由；若后续接入，权限待确认
- 当前完成度评估：低，代码存在但未接入
- 发现的问题：与实际在线首页 `AnnouncementHome.vue` 存在首页职责重叠；继续保留会增加维护成本和信息混乱
- 证据文件：`frontend2/src/views/Home.vue:3-135`；`frontend2/src/router/index.js:21-24`；`frontend2/src/components/Layout.vue:21`；`frontend2/src/api/activity.js:4-18`；`frontend2/src/api/activity.js:82-87`；`frontend2/src/api/user.js:21-26`

## 汇总

- 页面总数统计：已注册业务页面 18 个，其中普通端 11 个、管理端 7 个；布局壳路由 1 个（`/admin`）；未接入页面文件 1 个（`frontend2/src/views/Home.vue`）。
- 缺失页面列表：`公告列表页` 缺失，虽然有 `frontend2/src/api/announcement.js:11-20` 的 `getAnnouncementList` 和后端 `services/announcement-service/src/main/java/org/example/controller/AnnouncementController.java:54-58`，但当前没有任何路由/组件/菜单使用；`管理员全量报名列表页` 缺失，虽然有 `frontend2/src/api/activity.js:107-113` 的 `getAdminRegistrations` 和后端 `services/activity-service/src/main/java/org/example/controller/ActivityController.java:154-163`，但当前前端只有“按活动查看报名名单”的弹窗，没有独立页面承接。
- 功能重复页面列表：`frontend2/src/views/Home.vue` 与 `frontend2/src/views/AnnouncementHome.vue` 都承担首页入口职责，但只有后者接入 `/home`；`frontend2/src/views/admin/ActivityCreate.vue` 与 `frontend2/src/views/admin/ActivityManage.vue` 的编辑弹窗维护了两套几乎相同的活动表单、AI 生成和图片上传逻辑；`frontend2/src/views/FeedbackDetail.vue` 与 `frontend2/src/views/admin/FeedbackManage.vue` 抽屉都维护了消息时间线、附件列表、回复输入等相似实现。
- 建议优先重做页面列表：`frontend2/src/views/Profile.vue`，因为统计卡硬编码为 0 且资料更新未同步 `Pinia/localStorage`；`frontend2/src/views/admin/ActivityManage.vue`，因为缺搜索筛选、复用公共列表接口、还存在与创建页的大量重复代码；首页体系（`frontend2/src/views/AnnouncementHome.vue` 与未接入 `frontend2/src/views/Home.vue`）建议统一，只保留一条清晰首页路线并补齐真正的公告列表页；管理端报名管理建议补出独立页面，承接现有但未使用的 `getAdminRegistrations` 能力。
