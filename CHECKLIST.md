# 验收清单

## 基础环境

- [ ] MySQL 已启动
- [ ] Redis 已启动
- [ ] Nacos 已启动
- [ ] `database/init.sql` 已执行

## 后端服务

- [ ] `user-service` 监听 `8100`
- [ ] `activity-service` 监听 `8200`
- [ ] `gateway-service` 监听 `9000`
- [ ] `monitor-service` 监听 `9100`
- [ ] 以上服务均已注册到 Nacos

## 前端与 Nginx

- [ ] `frontend/dist` 已通过 `npm run build` 生成
- [ ] Nginx 已成功加载配置
- [ ] `/api/` 已转发到网关
- [ ] `/monitor/` 已转发到监控后台

## Docker 部署

- [ ] `docker-compose.yml` 已存在并可被 `docker compose config` 正确解析
- [ ] Docker Engine 已启动
- [ ] `docker compose up --build -d` 可执行
- [ ] `http://localhost:8080/` 可访问
- [ ] `http://localhost:8080/monitor/` 可访问
- [ ] `http://localhost:9001/actuator/health` 返回 `UP`
- [ ] `http://localhost:9101/actuator/health` 返回 `UP`
- [ ] `http://localhost:8849/nacos/` 可访问
- [ ] Docker 页面和接口返回的中文未出现乱码

## 页面访问

- [ ] `http://localhost/` 可访问
- [ ] `http://localhost/monitor/` 可访问
- [ ] `http://localhost:3000` 开发模式可访问

## 核心业务

- [ ] 管理员 `admin/password123` 可登录
- [ ] 志愿者账号可登录
- [ ] 活动列表可打开
- [ ] 活动详情可打开
- [ ] 报名接口可用
- [ ] 管理员可创建活动
- [ ] 管理员可签到
- [ ] 管理员可核销时长

## 局域网访问

- [ ] 本机 `80` 端口已放行
- [ ] 使用当前局域网 IPv4 可访问首页
- [ ] 使用当前局域网 IPv4 可访问 `/monitor/`

## 快速排查

端口检查：

```powershell
netstat -ano | findstr :80
netstat -ano | findstr :9000
netstat -ano | findstr :9100
```

接口检查：

```powershell
curl http://127.0.0.1/
curl http://127.0.0.1:9000/activity/list?page=1&size=10
curl http://127.0.0.1:9100/
```
