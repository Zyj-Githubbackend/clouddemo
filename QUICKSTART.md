# 🚀 快速开始指南

本指南将帮助你在 **10 分钟内** 快速启动校园志愿服务管理平台。

## 📋 前置检查清单

在开始之前，请确保已安装以下软件：

- ✅ JDK 17+
- ✅ Maven 3.6+
- ✅ MySQL 8.0+
- ✅ Redis 5.0+
- ✅ Nacos 2.x
- ✅ Node.js 16+ (前端)

## 🎯 快速启动步骤

### 步骤 1：启动 MySQL (2分钟)

1. 启动 MySQL 服务

2. 导入数据库
```bash
# 方式1：命令行导入
mysql -u root -p < database/init.sql

# 方式2：MySQL Workbench
# 打开 database/init.sql 文件执行
```

3. 验证数据库
```sql
USE volunteer_platform;
SHOW TABLES;
-- 应该看到: sys_user, vol_activity, vol_registration
```

**注意**：默认密码配置为 `123888`，如需修改请编辑各服务的 `application.properties`

### 步骤 2：启动 Redis (1分钟)

```bash
# Windows
redis-server.exe redis.windows.conf

# Linux/Mac
redis-server

# 验证
redis-cli ping
# 应该返回: PONG
```

### 步骤 3：启动 Nacos (2分钟)

1. 下载 Nacos（如果还没有）
```bash
# https://github.com/alibaba/nacos/releases
# 下载 nacos-server-2.x.x.zip
```

2. 启动 Nacos
```bash
# Windows
cd nacos/bin
startup.cmd -m standalone

# Linux/Mac
cd nacos/bin
sh startup.sh -m standalone
```

3. 验证 Nacos
- 访问：http://localhost:8848/nacos
- 登录账号：`nacos`
- 登录密码：`nacos`

### 步骤 4：编译后端项目 (2分钟)

在 **`services` 目录**下执行（与 `services/pom.xml` 一致）：
```bash
cd services
mvn clean install -DskipTests
```

或在**仓库根目录**执行（根 `pom.xml` 聚合构建 `services`）：
```bash
mvn clean install -DskipTests
```

**常见问题**：
- 如果编译失败，检查 Maven 配置和网络
- 确保使用 JDK 17
- 确保 `common` 模块配置了 `skip repackage`

### 步骤 5：启动后端服务 (3分钟)

**重要**：按照以下顺序启动服务

#### 5.1 启动 user-service (8100)

在 IDEA 中：
- 找到 `services/user-service/src/main/java/org/example/UserApplication.java`
- 右键 → Run 'UserApplication'

或命令行：
```bash
cd services/user-service
mvn spring-boot:run
```

**验证**：看到日志 `nacos registry, DEFAULT_GROUP user-service 192.168.40.1:8100 register finished`

#### 5.2 启动 activity-service (8200)

在 IDEA 中：
- 找到 `services/activity-service/src/main/java/org/example/ActivityApplication.java`
- 右键 → Run 'ActivityApplication'

或命令行：
```bash
cd services/activity-service
mvn spring-boot:run
```

**验证**：看到日志 `nacos registry, DEFAULT_GROUP activity-service ...`

**（可选）AI 生成活动描述**：需调用 DeepSeek 时，先设置环境变量 **`DEEPSEEK_API_KEY`** 再启动本服务；未设置则使用内置模板文案。详见 `README.md` →「AI / DeepSeek 配置」。

#### 5.3 启动 gateway-service (9000)

在 IDEA 中：
- 找到 `services/gateway-service/src/main/java/org/example/GatewayApplication.java`
- 右键 → Run 'GatewayApplication'

或命令行：
```bash
cd services/gateway-service
mvn spring-boot:run
```

**验证**：看到日志 `Netty started on port 9000`

#### 5.4 启动 monitor-service (9100，可选)

```bash
cd services/monitor-service
mvn spring-boot:run
```

### 步骤 6：启动前端 (2分钟)

```bash
cd frontend
npm install
npm run dev
```

**验证**：看到输出
```
VITE v5.4.21  ready in 323 ms
➜  Local:   http://localhost:3000/
```

## ✅ 验证安装

### 1. 检查 Nacos 服务注册

访问：http://localhost:8848/nacos

在 "服务管理" → "服务列表" 中应该看到：
- ✅ `user-service`
- ✅ `activity-service`
- ✅ `gateway-service`
- ✅ `monitor-service`（如果启动了）

### 2. 测试后端 API

```bash
# 测试登录接口
curl -X POST http://localhost:9000/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 应该返回 Token
```

### 3. 访问前端

打开浏览器访问：http://localhost:3000

你应该看到：
- 🎨 美观的登录页面
- 🌈 渐变背景 + 动态浮动元素
- ⚡ 三个快速登录标签

## 🎉 开始使用

### 快速登录测试

点击登录页面的快速登录标签即可自动填充账号密码：

| 标签 | 用户名 | 密码 | 角色 |
|------|--------|------|------|
| 🔴 管理员 | admin | password123 | 管理员 |
| 🟢 学生01 | student01 | password123 | 志愿者（12.5h） |
| 🟡 学生02 | student02 | password123 | 志愿者（8.0h） |

### 功能测试路径

#### 志愿者功能
1. 使用 `student01` 登录
2. 查看首页数据统计
3. 浏览志愿活动列表
4. 查看活动详情并报名
5. 在"个人中心"查看报名记录

#### 管理员功能
1. 使用 `admin` 登录
2. 进入"管理后台"
3. 发布新的志愿活动
4. （可选）使用 AI 生成活动描述（需配置 **`DEEPSEEK_API_KEY`**，见 README）
5. 核销志愿者的服务时长

## 🐛 常见问题排查

### 问题 1：服务启动失败

**现象**：服务启动报错或无法访问

**排查步骤**：
1. 检查端口是否被占用
```bash
# Windows
netstat -ano | findstr :8100
netstat -ano | findstr :9000

# Linux/Mac
lsof -i :8100
lsof -i :9000
```

2. 检查日志中的错误信息
3. 确认 MySQL、Redis、Nacos 都已启动

### 问题 2：服务无法注册到 Nacos

**现象**：Nacos 控制台看不到服务

**解决方案**：
1. 检查 Nacos 是否启动：访问 http://localhost:8848/nacos
2. 检查服务配置文件中的 Nacos 地址
```properties
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
```
3. 查看服务启动日志，确认注册信息
4. 重启服务

### 问题 3：前端登录 503 错误

**现象**：点击登录后提示 503 Service Unavailable

**解决方案**：
1. 确认 `user-service` 已启动
2. 确认 `gateway-service` 已启动
3. 在 Nacos 控制台查看服务是否注册成功
4. 检查 `gateway-service` 是否有 `spring-cloud-starter-loadbalancer` 依赖
5. 按顺序重启：user-service → gateway-service

### 问题 4：CORS 跨域错误

**现象**：浏览器控制台提示 CORS 错误

**解决方案**：
检查 `gateway-service/application.properties`：
```properties
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns=*
spring.cloud.gateway.globalcors.cors-configurations.[/**].allow-credentials=true
```

**注意**：使用 `allowed-origin-patterns` 而非 `allowed-origins`

### 问题 5：MyBatis-Plus 启动报错

**现象**：`BeanDefinitionStoreException` 或类似错误

**解决方案**：
1. 确认使用 Spring Boot 3 专版：
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>
```

2. 确认 `common` 模块的 `pom.xml` 中配置了：
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

3. 清理并重新编译：
```bash
cd services
mvn clean install -DskipTests
```

### 问题 6：前端无法加载

**现象**：npm run dev 报错或页面空白

**解决方案**：
1. 删除 `node_modules` 和 `package-lock.json`
```bash
rm -rf node_modules package-lock.json
npm install
```

2. 检查 Node.js 版本
```bash
node -v  # 应该是 16+
```

3. 检查端口 3000 是否被占用

## 📊 服务端口一览

| 服务 | 端口 | 访问地址 | 说明 |
|------|------|---------|------|
| Nacos | 8848 | http://localhost:8848/nacos | 服务注册中心 |
| user-service | 8100 | - | 用户服务 |
| activity-service | 8200 | - | 活动服务 |
| gateway-service | 9000 | http://localhost:9000 | API网关 |
| monitor-service | 9100 | http://localhost:9100 | 监控中心 |
| frontend | 3000 | http://localhost:3000 | 前端页面 |

## 🎯 下一步

- 📖 阅读 [系统架构文档](ARCHITECTURE.md) 了解技术架构
- 🔌 查看 [API测试文档](API_TEST.md) 测试各个接口
- 🚀 查看 [部署文档](DEPLOY.md) 部署到生产环境

## 💡 提示

1. **开发环境**：使用 IDEA 的 Run Dashboard 可以方便地管理多个服务
2. **调试技巧**：查看各服务的控制台日志排查问题
3. **响应式测试**：按 F12 → 设备模拟器测试移动端效果
4. **数据库工具**：推荐使用 Navicat 或 MySQL Workbench
5. **Redis工具**：推荐使用 RedisInsight 或 Another Redis Desktop Manager

## 📝 检查清单

启动完成后，请确认：

- [ ] MySQL 已启动且数据库已导入
- [ ] Redis 已启动
- [ ] Nacos 已启动并可访问
- [ ] 所有后端服务已注册到 Nacos
- [ ] 前端已启动并可访问
- [ ] 可以成功登录系统
- [ ] 可以浏览活动列表
- [ ] （管理员）可以发布活动

全部完成？恭喜你！🎉 现在可以开始探索系统的各项功能了！

---

💬 遇到问题？查看 [常见问题](README.md#常见问题) 或提交 Issue。
