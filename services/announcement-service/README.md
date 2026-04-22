# announcement-service

`announcement-service` 是公告微服务，负责首页公告、公告详情、管理员公告管理、公告图片与附件上传读取，以及公告和活动的关联展示。

## 基本信息

- 服务名：`announcement-service`
- 端口：`8300`
- 网关路径：`/announcement/**`
- 数据表：`vol_announcement`、`vol_announcement_activity`、`vol_announcement_attachment`

## 主要接口

公开或登录后用户常用接口：

- `GET /announcement/home`
- `GET /announcement/list`
- `GET /announcement/{id}`
- `GET /announcement/image?objectKey=...`
- `GET /announcement/attachment?objectKey=...`

管理员接口：

- `GET /announcement/admin/list`
- `GET /announcement/admin/{id}`
- `POST /announcement/admin`
- `PUT /announcement/admin/{id}`
- `POST /announcement/admin/{id}/publish`
- `POST /announcement/admin/{id}/offline`
- `DELETE /announcement/admin/{id}`
- `POST /announcement/admin/image`
- `POST /announcement/admin/attachment`

## 能力说明

- 公告支持 `PUBLISHED` 与 `OFFLINE` 状态。
- 公告图片支持多图，接口兼容 `imageKey` 与 `imageKeys`。
- 公告关联活动支持单个 `activityId` 和多个 `activityIds`。
- 公告附件支持 PDF、Excel、Word、TXT 和 CSV。
- 前端首页与公告详情会展示公告图片、附件和关联活动。

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
- Docker 单栈日志挂载目录：`log/docker/announcement-service/`
