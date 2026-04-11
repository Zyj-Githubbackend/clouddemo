# announcement-service

`announcement-service` 是公告微服务，负责首页公告、公告详情、管理员公告管理，以及公告图片上传与读取。

## 基本信息

- 服务名：`announcement-service`
- 端口：`8300`
- 网关路径：`/announcement/**`
- 数据表：`vol_announcement`

## 主要接口

公开或登录后用户常用接口：

- `GET /announcement/home`
- `GET /announcement/list`
- `GET /announcement/{id}`
- `GET /announcement/image?objectKey=...`

管理员接口：

- `GET /announcement/admin/list`
- `GET /announcement/admin/{id}`
- `POST /announcement/admin`
- `PUT /announcement/admin/{id}`
- `POST /announcement/admin/{id}/publish`
- `POST /announcement/admin/{id}/offline`
- `DELETE /announcement/admin/{id}`
- `POST /announcement/admin/image`

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

- 默认本机日志文件：`../../announcement-service/logs/debug.log`
- Docker 日志文件：`/app/logs/debug.log`
- Compose 挂载目录：`./announcement-service/logs`
