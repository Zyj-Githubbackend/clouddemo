# 验收清单

## 基础环境

- [ ] MySQL 已启动
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] MinIO 已启动
- [ ] `database/init.sql` 已执行

## 后端服务

- [ ] `user-service` 监听 `8100`
- [ ] `activity-service` 监听 `8200`
- [ ] `gateway-service` 监听 `9000`
- [ ] `monitor-service` 监听 `9100`
- [ ] `mcp-service` 监听 `9300`
- [ ] 以上服务均已注册或接入预期的运行链路

## 前端与 Nginx

- [ ] `frontend/dist` 已通过 `npm run build` 生成
- [ ] Nginx 已成功加载 `deploy/nginx/cloud-demo.local.conf`
- [ ] `/api/` 已转发到网关
- [ ] `/monitor/` 已转发到监控后台
- [ ] `/.well-known/*`、`/authorize`、`/token`、`/register`、`/mcp*` 已转发到 `mcp-service`

## 页面访问

- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
- [ ] `http://localhost/mcp` 未登录时返回 `401 Unauthorized`
- [ ] `http://localhost:3000` 开发模式可访问

## 核心用户流程

- [ ] 管理员 `admin/password123` 可登录
- [ ] 志愿者账号可登录
- [ ] 活动列表可打开并支持筛选
- [ ] 活动详情可打开
- [ ] 用户可报名活动
- [ ] 用户可取消未开始活动的报名
- [ ] 用户可在“我的志愿足迹”查看报名记录
- [ ] 用户可导出已核销志愿足迹 Excel

## 管理员流程

- [ ] 管理员可创建活动
- [ ] 管理员可编辑活动
- [ ] 管理员可取消活动
- [ ] 管理员可结项活动
- [ ] 管理员创建或编辑活动时可上传多张图片
- [ ] 活动详情页可展示多张图片
- [ ] 管理员可查看活动报名列表
- [ ] 管理员可查看当前可签到活动
- [ ] 管理员可执行签到
- [ ] 管理员可查看待核销活动
- [ ] 管理员可核销志愿时长
- [ ] 管理员可查看志愿时长统计

## MCP 接入

- [ ] `/.well-known/oauth-protected-resource` 返回正常
- [ ] `/.well-known/oauth-authorization-server` 返回正常
- [ ] `codex mcp add cloud-demo --url http://localhost/mcp` 可执行
- [ ] `codex mcp login cloud-demo` 可完成登录
- [ ] 普通用户登录后可调用查询类 MCP 工具
- [ ] 管理员登录后可调用活动管理、签到、核销类 MCP 工具
- [ ] 手动脚本 `scripts/mcp-login.ps1` 可获取并保存 token
- [ ] 手动脚本 `scripts/mcp-print-token.ps1` 可获取并打印 token

## Docker 部署

- [ ] `docker compose config` 可正确解析
- [ ] Docker Engine 已启动
- [ ] `docker compose up -d --build` 可执行
- [ ] `http://localhost:8081/` 可访问
- [ ] `http://localhost:8081/monitor/` 可访问
- [ ] `http://localhost:8081/mcp` 未登录时返回 `401 Unauthorized`
- [ ] `http://localhost:9001/actuator/health` 返回正常
- [ ] `http://localhost:9101/actuator/health` 返回正常
- [ ] `http://localhost:8849/nacos/` 可访问
- [ ] Docker 页面和接口返回的中文未出现乱码

## Jenkins

- [ ] Jenkins 可成功执行 `mvn -B test`
- [ ] Jenkins 可成功执行 `docker compose up -d --build`
- [ ] JUnit 报告可被 `**/target/surefire-reports/*.xml` 正确收集

## 快速排查命令

```powershell
netstat -ano | findstr :80
netstat -ano | findstr :9000
netstat -ano | findstr :9100
netstat -ano | findstr :9300
curl http://127.0.0.1/
curl "http://127.0.0.1:9000/activity/list?page=1&size=10"
curl http://127.0.0.1:9100/actuator/health
curl http://127.0.0.1:9300/actuator/health
```
