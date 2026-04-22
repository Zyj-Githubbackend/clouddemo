# 验收清单

## 本机部署

- [ ] MySQL 已启动并执行 `deploy/common/bootstrap-db.sql`
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] MinIO 已启动
- [ ] `user-service` 监听 `8100`
- [ ] `activity-service` 监听 `8200`
- [ ] `announcement-service` 监听 `8300`
- [ ] `feedback-service` 监听 `8400`
- [ ] `gateway-service` 监听 `9000`
- [ ] `monitor-service` 监听 `9100`
- [ ] `mcp-service` 监听 `9300`
- [ ] `frontend2` 可通过 `npm run dev` 访问
- [ ] Nginx 已加载 `deploy/local/nginx/cloud-demo.local.conf`
- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
- [ ] `http://localhost/mcp` 未登录时返回 `401 Unauthorized`

## Docker 单栈部署

- [ ] `docker compose -p cloud-demo -f deploy/docker/docker-compose.yml config` 可正确解析
- [ ] `powershell -ExecutionPolicy Bypass -File deploy/docker/up.ps1` 可启动
- [ ] `http://localhost:8081/` 可访问
- [ ] `http://localhost:8081/monitor/` 可访问
- [ ] `http://localhost:8081/mcp` 未登录时返回 `401 Unauthorized`
- [ ] `http://localhost:3000` 可访问 Grafana
- [ ] `http://localhost:9090` 可访问 Prometheus
- [ ] `http://localhost:8848/nacos` 可访问 Nacos
- [ ] `http://localhost:9006` 可访问 MinIO Console
- [ ] `log/docker/` 下有服务日志

## Kubernetes 部署

- [ ] `deploy/k8s/cloud-demo/secret.yaml` 已从 example 创建
- [ ] `powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/apply-all.ps1` 可执行
- [ ] `powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/init-db.ps1` 可执行
- [ ] `kubectl -n cloud-demo get pods` 全部核心 Pod Ready
- [ ] `kubectl -n observability get pods` 观测 Pod Ready
- [ ] `kubectl -n cloud-demo get ingress` 有 `cloud-demo.local`
- [ ] `kubectl -n observability get ingress` 有 Grafana 和 Prometheus host
- [ ] 本机 hosts 已添加 `cloud-demo.local`
- [ ] `http://cloud-demo.local:18081/` 可访问

## 核心用户流程

- [ ] 管理员 `admin/password123` 可登录
- [ ] 志愿者账号可登录
- [ ] 活动列表可打开并支持筛选
- [ ] 活动详情可打开
- [ ] 首页公告可打开并支持跳转关联活动详情
- [ ] 用户可报名活动
- [ ] 用户可取消未开始活动的报名
- [ ] 用户可导出已核销志愿足迹 Excel
- [ ] 用户可提交意见反馈
- [ ] 管理员可创建、编辑、取消和结项活动
- [ ] 管理员可签到和核销志愿时长
- [ ] 管理员可发布、编辑、下线和删除公告
- [ ] 管理员可处理反馈工单

## MCP 接入

- [ ] `/.well-known/oauth-protected-resource` 返回正常
- [ ] `/.well-known/oauth-authorization-server` 返回正常
- [ ] `codex mcp add cloud-demo --url http://localhost/mcp` 可执行
- [ ] `codex mcp login cloud-demo` 可完成登录
- [ ] 普通用户登录后可调用查询类 MCP 工具
- [ ] 管理员登录后可调用管理类 MCP 工具
