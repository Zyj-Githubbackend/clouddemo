# 🚀 部署文档

本文档介绍如何将校园志愿服务管理平台部署到生产环境。

**路径说明**：文中出现的 **`/opt/volunteer-platform`**、`/var/www/volunteer-platform` 等为**示例目录名**（与业务「志愿平台」对应）；本仓库 artifactId 为 **`cloud-demo`**，部署时可按需改为 `/opt/cloud-demo` 等一致路径。

## 📋 部署前准备

### 服务器要求

- **操作系统**：Linux（推荐 Ubuntu 20.04+ 或 CentOS 7+）
- **CPU**：2核心以上
- **内存**：4GB 以上
- **磁盘**：20GB 以上
- **网络**：公网 IP 或域名

### 软件环境

- **JDK**：17+
- **Maven**：3.6+
- **MySQL**：8.0+
- **Redis**：5.0+
- **Nacos**：2.x
- **Nginx**：1.18+（前端静态资源）
- **Docker**：20.10+（可选）

## 🎯 部署架构

```
           ┌─────────────┐
           │   Nginx     │ (80/443)
           │   反向代理   │
           └──────┬──────┘
                  │
        ┌─────────┴─────────┐
        │                   │
   ┌────▼────┐        ┌────▼────┐
   │  静态资源 │        │  Gateway │ (9000)
   │  (Vue)  │        │   网关   │
   └─────────┘        └────┬─────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼─────┐ ┌───▼────┐ ┌────▼─────┐
        │   User    │ │Activity│ │ Monitor  │
        │  Service  │ │Service │ │ Service  │
        │  (8100)   │ │ (8200) │ │ (9100)   │
        └─────┬─────┘ └───┬────┘ └──────────┘
              │           │
              └─────┬─────┘
                    │
           ┌────────┴────────┐
           │                 │
      ┌────▼────┐       ┌───▼───┐
      │  MySQL  │       │ Redis │
      │ (3306)  │       │ (6379)│
      └─────────┘       └───────┘
              │
         ┌────▼────┐
         │  Nacos  │
         │ (8848)  │
         └─────────┘
```

## 🐳 Docker 部署（推荐）

### 1. 准备 Dockerfile

#### 后端服务 Dockerfile

在 `services/` 目录下创建 `Dockerfile`：

```dockerfile
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 jar 包
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
```

#### 前端 Dockerfile

在 `frontend/` 目录下创建 `Dockerfile`：

```dockerfile
# 构建阶段
FROM node:16-alpine as build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 2. Docker Compose 配置

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0.45
    container_name: volunteer-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123888
      MYSQL_DATABASE: volunteer_platform
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      # 仅在数据目录为空时执行一次；已有数据卷时不会重复跑 init.sql，改库需删卷或手动导入
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - volunteer-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: volunteer-redis
    ports:
      - "6379:6379"
    networks:
      - volunteer-network

  # Nacos
  nacos:
    image: nacos/nacos-server:v2.4.2
    container_name: volunteer-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: 123888
    ports:
      - "8848:8848"
    depends_on:
      - mysql
    networks:
      - volunteer-network

  # User Service
  user-service:
    build:
      context: ./services/user-service
      dockerfile: Dockerfile
    container_name: volunteer-user-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/volunteer_platform
      SPRING_DATASOURCE_PASSWORD: 123888
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_DATA_REDIS_HOST: redis
    ports:
      - "8100:8100"
    depends_on:
      - mysql
      - redis
      - nacos
    networks:
      - volunteer-network

  # Activity Service
  activity-service:
    build:
      context: ./services/activity-service
      dockerfile: Dockerfile
    container_name: volunteer-activity-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/volunteer_platform
      SPRING_DATASOURCE_PASSWORD: 123888
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_DATA_REDIS_HOST: redis
    ports:
      - "8200:8200"
    depends_on:
      - mysql
      - redis
      - nacos
    networks:
      - volunteer-network

  # Gateway Service
  gateway-service:
    build:
      context: ./services/gateway-service
      dockerfile: Dockerfile
    container_name: volunteer-gateway-service
    environment:
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    ports:
      - "9000:9000"
    depends_on:
      - nacos
      - user-service
      - activity-service
    networks:
      - volunteer-network

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: volunteer-frontend
    ports:
      - "80:80"
    depends_on:
      - gateway-service
    networks:
      - volunteer-network

volumes:
  mysql-data:

networks:
  volunteer-network:
    driver: bridge
```

### 3. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 🖥️ 传统部署

### 1. 部署 MySQL

```bash
# 安装 MySQL
sudo apt update
sudo apt install mysql-server

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 初始化数据库（会删除已存在的 volunteer_platform 后整库重建）
mysql -u root -p < database/init.sql
```

`database/init.sql` 为**全量脚本**：含 `DROP DATABASE IF EXISTS volunteer_platform`、三张表、`v_activity_statistics` 视图及示例数据；合并了历史增量变更（如 `registration_start_time`），**无需再执行其他 SQL 文件**。

### 2. 部署 Redis

```bash
# 安装 Redis
sudo apt install redis-server

# 启动 Redis
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

### 3. 部署 Nacos

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/2.4.2/nacos-server-2.4.2.tar.gz

# 解压
tar -xzf nacos-server-2.4.2.tar.gz
cd nacos

# 修改配置（使用 MySQL 存储）
vim conf/application.properties

# 启动 Nacos（单机模式）
sh bin/startup.sh -m standalone
```

### 4. 部署后端服务

```bash
# 在仓库根目录编译（根 pom 聚合 services）
mvn clean package -DskipTests

# 若仅在 services 目录编译，则 jar 在 user-service/target/ 等，无需 services/ 前缀

# 创建服务目录
sudo mkdir -p /opt/volunteer-platform/{user-service,activity-service,gateway-service}

# 复制 jar 包（以下路径对应「根目录编译」）
sudo cp services/user-service/target/*.jar /opt/volunteer-platform/user-service/
sudo cp services/activity-service/target/*.jar /opt/volunteer-platform/activity-service/
sudo cp services/gateway-service/target/*.jar /opt/volunteer-platform/gateway-service/

# 创建启动脚本
sudo vim /opt/volunteer-platform/user-service/start.sh
```

**start.sh** 内容：
```bash
#!/bin/bash
nohup java -jar -Xms512m -Xmx1024m user-service-0.0.1-SNAPSHOT.jar > user-service.log 2>&1 &
echo $! > user-service.pid
```

```bash
# 赋予执行权限
sudo chmod +x /opt/volunteer-platform/user-service/start.sh

# 启动服务
cd /opt/volunteer-platform/user-service
./start.sh
```

**重复以上步骤** 部署 activity-service 和 gateway-service。

### 5. 配置 Systemd 服务（推荐）

创建 `/etc/systemd/system/volunteer-user.service`：

```ini
[Unit]
Description=Volunteer Platform - User Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/volunteer-platform/user-service
ExecStart=/usr/bin/java -jar -Xms512m -Xmx1024m user-service-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start volunteer-user
sudo systemctl enable volunteer-user

# 查看状态
sudo systemctl status volunteer-user
```

**重复以上步骤** 为其他服务创建 systemd 配置。

### 6. 部署前端

```bash
# 编译前端
cd frontend
npm install
npm run build

# 复制到 Nginx 目录
sudo cp -r dist/* /var/www/volunteer-platform/

# 配置 Nginx
sudo vim /etc/nginx/sites-available/volunteer-platform
```

**Nginx 配置**：
```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /var/www/volunteer-platform;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到 Gateway
    location /api/ {
        proxy_pass http://localhost:9000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 支持（如果需要）
    location /ws/ {
        proxy_pass http://localhost:9000/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/volunteer-platform /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

## 🔒 生产环境配置

### 1. 修改数据库密码

```properties
# application-prod.properties
spring.datasource.password=STRONG_PASSWORD_HERE
```

### 2. 修改 JWT Secret

```properties
jwt.secret=RANDOM_STRONG_SECRET_KEY_AT_LEAST_256_BITS
jwt.expiration=86400000
```

### 3. DeepSeek / AI 文案（activity-service）

生产环境**不要**把 API Key 写进仓库。在部署 **activity-service** 的进程环境中设置：

```bash
export DEEPSEEK_API_KEY="sk-你的密钥"
```

或在 `application-prod.properties` 中仅通过占位引用（由启动脚本注入），例如仍使用：

```properties
ai.api.url=https://api.deepseek.com/v1/chat/completions
ai.api.key=${DEEPSEEK_API_KEY:}
ai.api.model=deepseek-chat
```

未设置 `DEEPSEEK_API_KEY` 时服务正常启动，AI 接口返回模板兜底文案。详见 `README.md`「AI / DeepSeek 配置」。

### 4. 配置 HTTPS

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 5. 配置防火墙

```bash
# 允许 HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 禁止直接访问后端端口（仅允许本地）
sudo ufw deny 9000/tcp
sudo ufw deny 8100/tcp
sudo ufw deny 8200/tcp

# 启用防火墙
sudo ufw enable
```

### 6. 配置日志

修改 `application-prod.properties`：

```properties
# 日志配置
logging.level.root=INFO
logging.level.org.example=INFO
logging.file.name=/var/log/volunteer-platform/user-service.log
logging.file.max-size=10MB
logging.file.max-history=30
```

## 📊 监控与告警

### 1. Spring Boot Admin

访问 http://your-domain:9100 查看服务监控。

### 2. Prometheus + Grafana（可选）

```bash
# 添加 actuator 依赖
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
# 暴露 Prometheus 端点
management.endpoints.web.exposure.include=prometheus,health,info
management.metrics.export.prometheus.enabled=true
```

### 3. 日志监控

使用 ELK Stack（Elasticsearch + Logstash + Kibana）收集和分析日志。

## 🔄 CI/CD 自动化部署

### GitHub Actions 示例

创建 `.github/workflows/deploy.yml`：

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Build with Maven
      run: mvn -B clean package -DskipTests
    
    - name: Build Frontend
      run: |
        cd frontend
        npm install
        npm run build
    
    - name: Deploy to Server
      uses: appleboy/scp-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SERVER_SSH_KEY }}
        source: "services/*/target/*.jar,frontend/dist"
        target: "/tmp/volunteer-platform"
    
    - name: Restart Services
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SERVER_SSH_KEY }}
        script: |
          cd /opt/volunteer-platform
          ./deploy.sh
```

### （可选）PR 自动审批工作流

本仓库含 `.github/workflows/auto-approve-dev-to-main.yml`：目标分支为 **`main`**、源分支为 **`dev`** 的 PR，在 **可合并且无冲突** 时由 GitHub Actions 自动提交 **Approve**。请在仓库 **Settings → Actions → General** 中开启 **Read and write**，并勾选 **Allow GitHub Actions to create and approve pull requests**。  
**合并**仍需在 PR 上启用 **Auto-merge**（检查通过后由 GitHub 合并）或手动点 **Merge**；与 Jenkins 等外部 CI 并存时，注意审批与检查规则不要互相矛盾。

### Jenkins Pipeline 示例

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/your-repo/volunteer-platform.git'
            }
        }
        
        stage('Build Backend') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }
        
        stage('Build Frontend') {
            steps {
                sh 'cd frontend && npm install && npm run build'
            }
        }
        
        stage('Deploy') {
            steps {
                sh './deploy.sh'
            }
        }
    }
    
    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
```

## 📝 运维脚本

### 部署脚本 `deploy.sh`

```bash
#!/bin/bash

echo "Starting deployment..."

# 停止服务
sudo systemctl stop volunteer-user
sudo systemctl stop volunteer-activity
sudo systemctl stop volunteer-gateway

# 备份旧版本
BACKUP_DIR="/opt/volunteer-platform/backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp /opt/volunteer-platform/*/*.jar $BACKUP_DIR/

# 复制新版本
cp /tmp/volunteer-platform/services/user-service/target/*.jar /opt/volunteer-platform/user-service/
cp /tmp/volunteer-platform/services/activity-service/target/*.jar /opt/volunteer-platform/activity-service/
cp /tmp/volunteer-platform/services/gateway-service/target/*.jar /opt/volunteer-platform/gateway-service/

# 更新前端
sudo rm -rf /var/www/volunteer-platform/*
sudo cp -r /tmp/volunteer-platform/frontend/dist/* /var/www/volunteer-platform/

# 启动服务
sudo systemctl start volunteer-user
sleep 5
sudo systemctl start volunteer-activity
sleep 5
sudo systemctl start volunteer-gateway

# 检查服务状态
sudo systemctl status volunteer-user
sudo systemctl status volunteer-activity
sudo systemctl status volunteer-gateway

echo "Deployment completed!"
```

### 回滚脚本 `rollback.sh`

```bash
#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: ./rollback.sh <backup_timestamp>"
    exit 1
fi

BACKUP_DIR="/opt/volunteer-platform/backup/$1"

if [ ! -d "$BACKUP_DIR" ]; then
    echo "Backup not found: $BACKUP_DIR"
    exit 1
fi

echo "Rolling back to $BACKUP_DIR..."

# 停止服务
sudo systemctl stop volunteer-user
sudo systemctl stop volunteer-activity
sudo systemctl stop volunteer-gateway

# 恢复备份
cp $BACKUP_DIR/*.jar /opt/volunteer-platform/user-service/
cp $BACKUP_DIR/*.jar /opt/volunteer-platform/activity-service/
cp $BACKUP_DIR/*.jar /opt/volunteer-platform/gateway-service/

# 启动服务
sudo systemctl start volunteer-user
sleep 5
sudo systemctl start volunteer-activity
sleep 5
sudo systemctl start volunteer-gateway

echo "Rollback completed!"
```

## ✅ 部署检查清单

- [ ] 所有服务已编译打包
- [ ] MySQL 数据库已初始化
- [ ] Redis 已启动
- [ ] Nacos 已启动并可访问
- [ ] 所有后端服务已注册到 Nacos
- [ ] 前端已构建并部署到 Nginx
- [ ] Nginx 反向代理配置正确
- [ ] HTTPS 证书已配置（生产环境）
- [ ] 防火墙规则已配置
- [ ] 日志目录已创建并有写权限
- [ ] 监控服务已启动
- [ ] 备份策略已配置

## 🐛 故障排查

### 服务无法启动

```bash
# 查看日志
sudo journalctl -u volunteer-user -f

# 检查端口占用
sudo netstat -tunlp | grep 8100

# 检查内存使用
free -h
```

### 数据库连接失败

```bash
# 测试连接
mysql -h localhost -u root -p -e "SELECT 1"

# 检查 MySQL 状态
sudo systemctl status mysql
```

### Nacos 注册失败

```bash
# 检查 Nacos 状态
curl http://localhost:8848/nacos

# 查看 Nacos 日志
tail -f /path/to/nacos/logs/nacos.log
```

---

💡 **提示**：生产环境部署前请务必进行充分测试，建议先在测试环境验证后再部署到生产环境。
