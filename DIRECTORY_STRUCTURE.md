# 项目目录结构

```
cloud-demo/                                    # 项目根目录
│
├── .idea/                                     # IDEA配置目录（自动生成）
├── .gitignore                                 # Git忽略配置
│
├── pom.xml                                    # 父POM文件
│   └── Spring Boot 3.3.4
│   └── Spring Cloud Alibaba 2023.0.3.2
│
├── database/                                  # 数据库脚本目录
│   └── init.sql                              # 数据库初始化脚本
│       ├── volunteer_platform数据库
│       ├── sys_user表（用户表）
│       ├── vol_activity表（活动表）
│       ├── vol_registration表（报名表）
│       └── v_activity_statistics视图
│
├── logs/                                      # 日志目录
│   ├── monitor-service.log
│   ├── gateway-service.log
│   ├── user-service.log
│   └── activity-service.log
│
├── services/                                  # 微服务模块目录
│   │
│   ├── pom.xml                               # services父POM
│   │
│   ├── common/                               # 【公共模块】
│   │   ├── pom.xml
│   │   └── src/main/java/org/example/common/
│   │       ├── result/
│   │       │   └── Result.java               # 统一返回结果封装
│   │       ├── util/
│   │       │   └── JwtUtil.java              # JWT工具类
│   │       ├── exception/
│   │       │   └── BusinessException.java    # 业务异常
│   │       └── constant/
│   │           └── RedisKeyConstant.java     # Redis Key常量
│   │
│   ├── gateway-service/                      # 【网关服务】端口: 9000
│   │   ├── pom.xml
│   │   │   ├── spring-cloud-starter-gateway
│   │   │   ├── nacos-discovery
│   │   │   └── common模块
│   │   └── src/main/
│   │       ├── java/org/example/
│   │       │   ├── GatewayApplication.java   # 启动类
│   │       │   ├── filter/
│   │       │   │   └── AuthFilter.java       # JWT认证过滤器
│   │       │   └── config/
│   │       │       └── CorsConfig.java       # 跨域配置
│   │       └── resources/
│   │           └── application.properties     # 路由配置
│   │               ├── server.port=9000
│   │               ├── 路由规则配置
│   │               └── JWT配置
│   │
│   ├── user-service/                         # 【用户服务】端口: 8100
│   │   ├── pom.xml
│   │   │   ├── spring-boot-starter-web
│   │   │   ├── mybatis-plus
│   │   │   ├── mysql-connector
│   │   │   └── common模块
│   │   └── src/main/
│   │       ├── java/org/example/
│   │       │   ├── UserApplication.java      # 启动类
│   │       │   ├── entity/
│   │       │   │   └── User.java             # 用户实体
│   │       │   ├── dto/
│   │       │   │   ├── LoginRequest.java     # 登录请求DTO
│   │       │   │   └── RegisterRequest.java  # 注册请求DTO
│   │       │   ├── vo/
│   │       │   │   ├── LoginResponse.java    # 登录响应VO
│   │       │   │   └── UserInfo.java         # 用户信息VO
│   │       │   ├── mapper/
│   │       │   │   └── UserMapper.java       # MyBatis Mapper
│   │       │   ├── service/
│   │       │   │   └── UserService.java      # 业务逻辑层
│   │       │   ├── controller/
│   │       │   │   ├── UserController.java           # 用户控制器
│   │       │   │   └── InternalUserController.java   # 内部调用接口
│   │       │   └── exception/
│   │       │       └── GlobalExceptionHandler.java   # 全局异常处理
│   │       └── resources/
│   │           └── application.properties     # 配置文件
│   │               ├── server.port=8100
│   │               ├── MySQL配置
│   │               ├── MyBatis-Plus配置
│   │               └── Nacos配置
│   │
│   ├── activity-service/                     # 【活动服务】端口: 8200
│   │   ├── pom.xml
│   │   │   ├── spring-boot-starter-web
│   │   │   ├── mybatis-plus
│   │   │   ├── mysql-connector
│   │   │   ├── spring-boot-starter-data-redis
│   │   │   ├── spring-cloud-starter-openfeign
│   │   │   └── common模块
│   │   └── src/main/
│   │       ├── java/org/example/
│   │       │   ├── ActivityApplication.java   # 启动类
│   │       │   ├── entity/
│   │       │   │   ├── Activity.java          # 活动实体
│   │       │   │   └── Registration.java      # 报名实体
│   │       │   ├── dto/
│   │       │   │   ├── ActivityCreateRequest.java  # 创建活动DTO
│   │       │   │   └── AIGenerateRequest.java      # AI生成请求DTO
│   │       │   ├── vo/
│   │       │   │   ├── ActivityVO.java        # 活动VO
│   │       │   │   └── RegistrationVO.java    # 报名VO
│   │       │   ├── mapper/
│   │       │   │   ├── ActivityMapper.java    # 活动Mapper
│   │       │   │   └── RegistrationMapper.java # 报名Mapper
│   │       │   ├── service/
│   │       │   │   ├── ActivityService.java   # 核心业务逻辑
│   │       │   │   └── AIService.java         # AI服务
│   │       │   ├── feign/
│   │       │   │   └── UserServiceClient.java # Feign客户端
│   │       │   ├── controller/
│   │       │   │   └── ActivityController.java # 活动控制器
│   │       │   └── exception/
│   │       │       └── GlobalExceptionHandler.java
│   │       └── resources/
│   │           └── application.properties      # 配置文件
│   │               ├── server.port=8200
│   │               ├── MySQL配置
│   │               ├── Redis配置
│   │               ├── DeepSeek（OpenAI 兼容）API 配置，DEEPSEEK_API_KEY
│   │               └── Nacos配置
│   │
│   ├── monitor-service/                      # 【监控服务】端口: 9100
│   │   ├── pom.xml
│   │   │   ├── spring-boot-admin-starter-server
│   │   │   ├── spring-boot-starter-web
│   │   │   └── nacos-discovery
│   │   └── src/main/
│   │       ├── java/org/example/
│   │       │   └── MonitorApplication.java    # 启动类
│   │       └── resources/
│   │           └── application.properties      # 配置文件
│   │               ├── server.port=9100
│   │               └── Spring Boot Admin配置
│
├── 📄 文档与 CI（仓库根目录）
├── README.md
├── ARCHITECTURE.md
├── DEPLOY.md
├── API_TEST.md
├── QUICKSTART.md
├── PROJECT_SUMMARY.md
├── CHECKLIST.md
├── .github/workflows/                        # GitHub Actions 工作流
│
└── 🛠️ 工具脚本
    ├── start-all.bat                         # Windows 启动（顺序：monitor→gateway→user→activity）
    └── start-all.sh                          # Linux 启动（同上）
```

## 关键目录说明

### 1. services/common/ - 公共模块
所有微服务共享的代码，避免重复：
- 统一返回结果封装
- JWT工具类
- 业务异常定义
- Redis Key常量

### 2. services/gateway-service/ - 网关服务
系统的统一入口：
- 路由转发到各微服务
- JWT Token验证
- 跨域CORS处理
- 请求头注入用户信息

### 3. services/user-service/ - 用户服务
用户相关的所有功能：
- 用户注册/登录
- JWT Token生成
- 用户信息管理
- 志愿时长累计

### 4. services/activity-service/ - 活动服务
核心业务服务：
- 活动CRUD
- 报名管理（Redis防超卖）
- 时长核销
- AI智能生成文案
- Feign调用用户服务

### 5. services/monitor-service/ - 监控服务
服务健康监控：
- 自动发现Nacos服务
- JVM性能监控
- 实时日志查看
- HTTP请求追踪

## 文件数量统计

| 类型 | 数量 | 说明 |
|------|------|------|
| Java源文件 | 34 | services 下业务与公共代码 |
| POM文件 | 7 | 包括父POM |
| 配置文件 | 5 | application.properties |
| SQL脚本 | 1 | init.sql |
| 文档文件 | 6 | Markdown文档 |
| 启动脚本 | 2 | bat + sh |

## 代码行数统计（估算）

| 模块 | Java代码 | 配置代码 | 文档 |
|------|----------|----------|------|
| common | ~200 行 | - | - |
| gateway-service | ~150 行 | ~30 行 | - |
| user-service | ~350 行 | ~40 行 | - |
| activity-service | ~600 行 | ~50 行 | - |
| monitor-service | ~20 行 | ~20 行 | - |
| 数据库脚本 | - | ~200 行 | - |
| 文档 | - | - | ~2000 行 |
| **总计** | **~1320 行** | **~340 行** | **~2000 行** |

## 核心文件清单

### 启动类 (5个)
1. `GatewayApplication.java` - 网关启动类
2. `UserApplication.java` - 用户服务启动类
3. `ActivityApplication.java` - 活动服务启动类
4. `MonitorApplication.java` - 监控服务启动类
5. (common模块无启动类)

### Controller (4个)
1. `UserController.java` - 3个接口
2. `InternalUserController.java` - 1个内部接口
3. `ActivityController.java` - 活动/报名/核销/AI 等 REST 接口

### Service (4个)
1. `UserService.java` - 用户业务逻辑
2. `ActivityService.java` - 活动业务逻辑
3. `AIService.java` - AI服务逻辑

### Mapper (3个)
1. `UserMapper.java`
2. `ActivityMapper.java`
3. `RegistrationMapper.java`

### Entity (3个)
1. `User.java`
2. `Activity.java`
3. `Registration.java`

### Filter/Config (2个)
1. `AuthFilter.java` - JWT认证过滤器
2. `CorsConfig.java` - 跨域配置

## 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 注册中心 |
| Redis | 6379 | 缓存 |
| MySQL | 3306 | 数据库 |
| gateway-service | 9000 | 网关 |
| user-service | 8100 | 用户服务 |
| activity-service | 8200 | 活动服务 |
| monitor-service | 9100 | 监控中心 |

## 依赖关系图

```
┌─────────────────┐
│  gateway-service│
│    (9000)       │
└────────┬────────┘
         │ 依赖
         ▼
    ┌────────┐
    │ common │◄──────────┐
    └────────┘           │
         ▲               │
         │ 依赖          │ 依赖
┌────────┴────────┐  ┌───┴──────────────┐
│  user-service   │  │ activity-service │
│    (8100)       │◄─┤    (8200)        │
└─────────────────┘  └──────────────────┘
                         Feign调用

    ┌──────────────────┐
    │ monitor-service  │
    │    (9100)        │
    └──────────────────┘
         独立监控
```

## 快速导航

- **查看项目介绍**: `README.md`
- **了解架构设计**: `ARCHITECTURE.md`
- **学习部署步骤**: `DEPLOY.md`
- **测试API接口**: `API_TEST.md`
- **快速启动**: `QUICKSTART.md`
- **项目总结**: `PROJECT_SUMMARY.md`
