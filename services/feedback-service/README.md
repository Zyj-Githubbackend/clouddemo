# feedback-service

`feedback-service` 是意见反馈微服务，负责用户反馈工单、消息回复、附件上传下载，以及管理员反馈处理。

## 基本信息

- 服务名：`feedback-service`
- 端口：`8400`
- 网关路径：`/feedback/**`
- 数据表：`feedback`、`feedback_message`、`feedback_message_attachment`

## 主要接口

用户接口：

- `POST /feedback`
- `GET /feedback/my`
- `GET /feedback/{id}`
- `POST /feedback/{id}/messages`
- `POST /feedback/{id}/close`
- `POST /feedback/attachments`
- `GET /feedback/attachments?objectKey=...`

管理员接口：

- `GET /feedback/admin/list`
- `GET /feedback/admin/{id}`
- `POST /feedback/admin/{id}/messages`
- `POST /feedback/admin/{id}/close`
- `POST /feedback/admin/{id}/reject`
- `POST /feedback/admin/{id}/priority`

## 状态与枚举

分类：

- `QUESTION`
- `SUGGESTION`
- `BUG`
- `COMPLAINT`
- `OTHER`

状态：

- `OPEN`
- `REPLIED`
- `CLOSED`
- `REJECTED`

优先级：

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

附件：

- 支持 JPG、PNG、GIF、WEBP、PDF、Excel、Word、TXT、CSV
- 默认大小上限为 5 MB，可通过 `MINIO_MAX_FILE_SIZE_MB` 调整
- 单条消息最多绑定 6 个附件
- 下载前会校验访问权限，普通用户只能访问自己的反馈附件，管理员可访问全部反馈附件

## 环境变量

- `APP_LOG_FILE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_CLOUD_NACOS_SERVER_ADDR`
- `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `MINIO_PUBLIC_BASE_URL`
- `MINIO_MAX_FILE_SIZE_MB`

## 日志

- 默认本机日志文件：按运行方式决定，可通过 `APP_LOG_FILE` 覆盖
- Docker 日志文件：`/app/logs/debug.log`
- Docker 单栈日志挂载目录：`log/docker/feedback-service/`

## 数据库迁移

新环境使用统一初始化脚本：

```bash
mysql -u root -p < deploy/common/bootstrap-db.sql
```

Docker 默认启动时会通过 `db-init` 服务执行统一初始化脚本；如需手动重放：

```bash
docker compose run --rm db-init
```
