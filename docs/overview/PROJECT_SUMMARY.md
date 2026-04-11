# 项目交付摘要

## 当前状态

项目已经具备完整的本机运行与 Docker 运行链路：

- 后端微服务可独立启动
- 前端可开发模式运行，也可构建后交给 Nginx 托管
- `monitor-service` 支持通过 `/monitor/` 反向代理访问
- `mcp-service` 支持通过 `/mcp` 对外提供 MCP 能力
- Docker Compose 可一键启动 MySQL、Redis、Nacos、MinIO、前端与全部后端服务
- 本机 Jenkins 可先执行 `mvn -B test` 再执行 `docker compose up -d --build`

## 已交付模块

| 模块 | 说明 |
|------|------|
| `services/common` | 统一结果结构、异常与 JWT 工具 |
| `services/user-service` | 注册登录、资料维护、志愿时长查询 |
| `services/activity-service` | 活动管理、报名、签到、时长核销、AI 文案、图片上传、Excel 导出 |
| `services/gateway-service` | 统一入口、JWT 鉴权、请求头透传 |
| `services/monitor-service` | Spring Boot Admin 监控中心 |
| `services/mcp-service` | 独立 MCP Server，封装用户与活动相关工具 |
| `frontend` | Vue 3 前端页面与后台管理界面 |
| `database/init.sql` | 数据库结构、默认账号、活动与报名示例数据 |
| `deploy/nginx` | 本机与容器代理配置 |
| `docker-compose.yml` | 一键容器部署入口 |
| `scripts/mcp-login.ps1` | MCP 手动 token 登录并写入环境变量的辅助脚本 |
| `scripts/mcp-print-token.ps1` | MCP 手动 token 获取并打印的辅助脚本 |

## 当前推荐访问方式

### 本机 Nginx 模式

- 前台：`http://localhost/`
- 监控后台：`http://localhost/monitor/`
- MCP：`http://localhost/mcp`
- 开发模式前端：`http://localhost:3000`

### Docker 模式

- 前台：`http://localhost:8081/`
- 监控后台：`http://localhost:8081/monitor/`
- MCP：`http://localhost:8081/mcp`
- 网关直连：`http://localhost:9001`
- 监控直连：`http://localhost:9101`
- Nacos：`http://localhost:8849/nacos`

## 最近对齐的能力

- 活动多图上传与详情多图展示
- 用户取消未开始活动的报名
- 用户导出已核销志愿足迹 Excel
- `activity/list` 支持 `status`、`category`、`recruitmentPhase` 筛选
- `mcp-service` 支持 OAuth 授权码登录
- MCP 工具已覆盖报名、资料、导出、活动管理、签到和批量核销场景

## 示例数据说明

- 默认账号：`admin` 与 `student01` 到 `student10`
- 默认密码：`password123`
- 默认活动数据：20 条
- 种子数据围绕 `2026-03-25` 设计，便于覆盖“未开始、招募中、招募结束、进行中、已完成、已取消”等场景

## 自动化测试现状

当前仓库已包含以下单元测试：

- `services/common/src/test/java/org/example/common/util/JwtUtilTest.java`
- `services/user-service/src/test/java/org/example/service/UserServiceTest.java`
- `services/activity-service/src/test/java/org/example/service/ActivityScheduleValidatorTest.java`
- `services/activity-service/src/test/java/org/example/service/ActivityServiceTest.java`

## 建议阅读顺序

1. [README.md](../../README.md)
2. [快速开始](../setup/QUICKSTART.md)
3. [部署说明](../deploy/DEPLOY.md)
4. [API 测试](../testing/API_TEST.md)
5. [MCP 连接说明](../mcp/MCP_CONNECTION.md)
