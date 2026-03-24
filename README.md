# 🌟 校园志愿服务管理平台

> 基于 Spring Boot 3.3.4 + Spring Cloud Alibaba + Vue 3 的现代化微服务志愿服务管理系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.3.2-blue.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📖 项目简介

校园志愿服务管理平台是一个功能完善、界面美观的志愿服务管理系统，支持志愿活动发布、在线报名、时长核销、数据统计等核心功能。系统采用前后端分离架构，具备高可用性、高扩展性和良好的用户体验。

### ✨ 核心特性

- 🎨 **现代化UI设计** - 响应式布局，支持桌面端、平板、移动端
- 🔐 **JWT安全认证** - API网关统一鉴权，角色权限控制
- 🚀 **微服务架构** - 服务注册与发现，负载均衡，高可用
- 📊 **实时数据统计** - 个人志愿时长、活动参与度等数据可视化
- 🤖 **AI智能辅助** - 支持AI生成活动描述（可选）
- 🔄 **防超卖机制** - Redis分布式锁保证活动报名准确性
- 📱 **移动端适配** - 完美支持手机、平板等移动设备
- ⚡ **快速登录** - 开发/测试环境一键切换账号

## 🏗️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.4 | 基础框架（Java 17） |
| Spring Cloud Alibaba | 2023.0.3.2 | 微服务治理 |
| Nacos | 2.4.2 | 服务注册与配置中心 |
| Spring Cloud Gateway | 4.1.5 | API网关 |
| Spring Cloud LoadBalancer | 4.1.4 | 负载均衡 |
| MyBatis-Plus | 3.5.9 | ORM框架（Spring Boot 3专版） |
| MySQL | 8.0.45 | 关系型数据库 |
| Redis | 5.0+ | 缓存与分布式锁 |
| JWT | 0.11.5 | 认证令牌 |
| OpenFeign | 4.1.3 | 服务间调用 |
| Spring Boot Admin | 3.3.4 | 服务监控 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4 | 渐进式框架 |
| Vite | 5.4 | 构建工具 |
| Element Plus | 最新版 | UI组件库 |
| Pinia | 最新版 | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 最新版 | HTTP客户端 |

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 5.0+
- **Node.js**: 16+
- **Nacos**: 2.x

### 1. 启动基础服务

#### 启动 MySQL
```bash
# 导入数据库
mysql -u root -p < database/init.sql
# 默认密码: 123888
```

#### 启动 Redis
```bash
redis-server
```

#### 启动 Nacos
```bash
# Windows
cd nacos/bin
startup.cmd -m standalone

# Linux/Mac
cd nacos/bin
sh startup.sh -m standalone
```

访问 Nacos 控制台：http://localhost:8848/nacos
- 默认账号：`nacos`
- 默认密码：`nacos`

### 2. 启动后端服务

在 IDEA 中按顺序启动：

1. **UserApplication** (8100端口) - 用户服务
2. **ActivityApplication** (8200端口) - 活动服务  
3. **GatewayApplication** (9000端口) - API网关
4. **MonitorApplication** (9100端口，可选) - 监控中心

或使用命令行：
```bash
cd services
mvn clean install -DskipTests

# 启动各个服务
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar
java -jar activity-service/target/activity-service-0.0.1-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问前端：http://localhost:3000

### 4. 登录测试

系统提供 **3 个测试账号**：

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin` | `password123` | 可发布活动、核销时长 |
| 志愿者 | `student01` | `password123` | 张三，已有12.5小时 |
| 志愿者 | `student02` | `password123` | 李四，已有8.0小时 |

💡 **提示**：登录页面有快速登录标签，点击即可自动填充账号密码！

## 📁 项目结构

```
cloud-demo/
├── services/                    # 后端服务
│   ├── common/                  # 公共模块
│   │   └── src/main/java/org/example/common/
│   │       ├── result/          # 统一返回格式
│   │       ├── util/            # JWT工具类
│   │       ├── exception/       # 自定义异常
│   │       └── constant/        # 常量定义
│   ├── gateway-service/         # API网关 (9000)
│   │   └── src/main/java/org/example/
│   │       ├── filter/          # JWT认证过滤器
│   │       └── config/          # CORS配置
│   ├── user-service/           # 用户服务 (8100)
│   │   └── src/main/java/org/example/
│   │       ├── entity/          # User实体
│   │       ├── dto/             # 请求DTO
│   │       ├── vo/              # 响应VO
│   │       ├── mapper/          # MyBatis Mapper
│   │       ├── service/         # 业务逻辑
│   │       └── controller/      # REST接口
│   ├── activity-service/       # 活动服务 (8200)
│   │   └── src/main/java/org/example/
│   │       ├── entity/          # Activity, Registration实体
│   │       ├── dto/             # 请求DTO
│   │       ├── vo/              # 响应VO
│   │       ├── mapper/          # MyBatis Mapper
│   │       ├── service/         # 业务逻辑 + AI服务
│   │       ├── feign/           # Feign调用user-service
│   │       └── controller/      # REST接口
│   └── monitor-service/        # 监控中心 (9100)
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   │   ├── Login.vue       # 登录页（美化版）
│   │   │   ├── Register.vue    # 注册页
│   │   │   ├── Home.vue        # 首页（响应式）
│   │   │   ├── ActivityList.vue
│   │   │   ├── ActivityDetail.vue
│   │   │   ├── MyCenter.vue
│   │   │   └── admin/          # 管理员页面
│   │   ├── components/
│   │   │   └── Layout.vue      # 布局组件（响应式导航）
│   │   ├── api/                # API接口
│   │   ├── router/             # 路由配置
│   │   ├── store/              # Pinia状态管理
│   │   ├── utils/              # 工具函数
│   │   └── style.css           # 全局样式
│   └── vite.config.js          # Vite配置
├── database/                    # 数据库脚本
│   └── init.sql                # 初始化SQL（含测试数据）
└── docs/                       # 文档
    ├── QUICKSTART.md           # 快速开始
    ├── ARCHITECTURE.md         # 架构文档
    ├── API_TEST.md             # API测试
    └── DEPLOY.md               # 部署文档
```

## 🎨 界面预览

### 登录页面
- ✨ 渐变背景 + 动态浮动元素
- 🔐 现代化输入框设计
- ⚡ 快速登录标签（点击即填充）
- 📱 完全响应式布局

### 首页
- 📊 数据统计卡片（4个指标）
- 🎴 活动卡片网格（悬浮效果）
- ✨ 流畅的动画过渡
- 📱 移动端自适应（2列 → 1列）

### 移动端
- 📱 抽屉式侧边栏导航
- 🎯 触摸友好的按钮
- 📐 灵活的响应式网格
- 🔄 流畅的动画效果

## 🔧 配置说明

### 数据库配置
修改各服务的 `application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/volunteer_platform
spring.datasource.username=root
spring.datasource.password=123888
```

### Redis配置
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Nacos配置
```properties
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
```

### JWT配置
```properties
jwt.secret=volunteerPlatformSecretKey2024
jwt.expiration=86400000
```

## 📊 功能模块

### 志愿者功能
- ✅ 浏览志愿活动列表
- ✅ 查看活动详情
- ✅ 在线报名参加
- ✅ 查看个人报名记录
- ✅ 查看累计志愿时长
- ✅ 个人数据统计

### 管理员功能
- ✅ 发布志愿活动
- ✅ AI智能生成活动描述
- ✅ 活动状态管理
- ✅ 志愿时长核销
- ✅ 报名信息查看
- ✅ 数据统计分析

### 系统功能
- ✅ JWT令牌认证
- ✅ 角色权限控制
- ✅ Redis防超卖机制
- ✅ 服务注册发现
- ✅ 负载均衡
- ✅ 健康检查监控

## 🐛 常见问题

### 1. 服务无法注册到 Nacos
- 检查 Nacos 是否启动：`http://localhost:8848/nacos`
- 检查配置文件中的 Nacos 地址是否正确
- 查看服务启动日志中的注册信息

### 2. 登录提示 503 错误
- 确认 user-service 已启动且注册到 Nacos
- 确认 gateway-service 已添加 LoadBalancer 依赖
- 在 Nacos 控制台查看服务列表
- 重启 gateway-service

### 3. 前端无法访问后端
- 检查 Vite 代理配置（vite.config.js）
- 确认 gateway 运行在 9000 端口
- 检查浏览器控制台的网络请求
- 查看 CORS 配置是否正确

### 4. MyBatis-Plus 启动报错
- 必须使用 `mybatis-plus-spring-boot3-starter` 而非 `mybatis-plus-boot-starter`
- 版本使用 3.5.9
- common 模块需配置 `skip repackage`

### 5. Gateway CORS 错误
- 使用 `allowed-origin-patterns` 而非 `allowed-origins`
- 配置 `allow-credentials=true`

## 📝 开发规范

### 后端规范
- 统一返回 `Result<T>` 格式
- 使用 JWT 进行身份验证
- 异常统一处理
- 日志规范记录
- RESTful API 设计

### 前端规范
- 组件化开发
- 响应式布局（支持手机/平板/桌面）
- 统一的样式规范（渐变主题色）
- API 请求统一封装
- 路由守卫鉴权

### 响应式断点
```css
手机: < 480px
平板: 480px - 768px  
桌面: 768px - 1024px
大屏: > 1024px
```

## 🔒 安全机制

### JWT认证流程
```
1. 用户登录 → 生成JWT Token
2. 前端存储Token（localStorage）
3. 每次请求携带Token（Authorization: Bearer {token}）
4. Gateway验证Token
5. 解析用户信息注入请求头
6. 下游服务获取用户信息
```

### 白名单接口
- `/user/login` - 登录
- `/user/register` - 注册
- `/activity/list` - 活动列表

## 🎯 核心业务流程

### 活动报名流程（防超卖）
```
1. 管理员创建活动 → vol_activity表
2. 初始化Redis库存 → activity:stock:{id}
3. 志愿者浏览活动列表
4. 点击报名 → Redis原子减库存
5. 库存充足 → 写入vol_registration表
6. 更新活动当前人数
```

### 时长核销流程
```
1. 活动结束
2. 管理员核销时长
3. 更新报名记录 → hours_confirmed=1
4. Feign调用user-service更新总时长
5. 志愿者查看累计时长
```

## 📚 相关文档

- [快速开始指南](QUICKSTART.md)
- [系统架构文档](ARCHITECTURE.md)
- [API测试文档](API_TEST.md)
- [部署指南](DEPLOY.md)
- [目录结构说明](DIRECTORY_STRUCTURE.md)
- [项目检查清单](CHECKLIST.md)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📮 联系方式

如有问题或建议，欢迎通过 Issue 反馈。

## 🌟 致谢

感谢所有贡献者和开源社区的支持！

---

⭐ 如果这个项目对你有帮助，请给个 Star！
