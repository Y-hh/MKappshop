MKappshop 商城
项目简介
MKappshop 是一个基于 Spring Boot 3 + JPA 的 B2C 商城后端，同时服务于微信小程序用户和管理员后台。项目采用模块化设计，区分 wx（微信小程序）和 admin（管理后台）两条业务线，支持 JWT 身份认证、雪花ID主键、全局异常处理等特性。

当前版本已完成商品管理、用户认证基础框架，正在完善订单及购物车模块。

技术栈
领域	技术选型
核心框架	Spring Boot 3.x
持久层	Spring Data JPA + Hibernate
数据库	MySQL 8.0+
身份认证	JWT (jjwt)
工具库	Lombok, Hutool
构建工具	Maven
版本控制	Git
功能模块
微信小程序端 (wx)
用户认证（微信登录、手机号解密）

商品浏览

购物车管理（待开发）

订单流程（待开发）

管理后台端 (admin)
管理员登录

商品管理（增删改查）

订单管理（待开发）

用户管理（待开发）

项目结构
text
com.MKappshop.MKappshop
├── MKappshopApplication.java
│
├── annotation
│   ├── CurrentUser.java          (⚠️ 待完善)
│   └── CurrentAdmin.java         (✅ 已完成)
│
├── common
│   ├── Result.java               (✅ 已完成)
│   └── constant/                 (📁 待建)
│
├── config
│   ├── SecurityConfig.java       (✅ 已完成)
│   ├── RedisConfig.java          (⚠️ 待补充)
│   └── WebMvcConfig.java         (⚠️ 待补充)
│
├── controller
│   ├── wx
│   │   └── AuthController.java   (⚠️ 待重构)
│   ├── admin
│   │   └── AdminAuthController.java (✅ 已完成)
│   ├── product
│   │   └── ProductController.java (✅ 已完成)
│   └── order
│       └── OrderController.java  (📁 待开发)
│
├── dto
│   ├── wx
│   │   ├── WxLoginRequest.java   (✅ 已完成)
│   │   ├── WxLoginResponse.java  (✅ 已完成)
│   │   └── DecryptPhoneRequest.java (✅ 已完成)
│   ├── admin
│   │   ├── AdminLoginRequest.java (✅ 已完成)
│   │   └── AdminLoginResponse.java (✅ 已完成)
│   └── product
│       └── ProductDTO.java       (✅ 已完成)
│
├── entity
│   ├── wx
│   │   └── WxUser.java           (✅ 已完成 - 雪花ID)
│   ├── admin
│   │   └── Admin.java            (✅ 已完成 - 自增ID)
│   ├── product
│   │   └── Product.java          (✅ 已完成 - 雪花ID)
│   └── order
│       ├── Order.java            (✅ 已完成 - 雪花ID)
│       └── OrderItem.java        (✅ 已完成 - 雪花ID)
│
├── exception
│   ├── BizException.java         (✅ 已完成)
│   └── ResourceNotFoundException.java (✅ 已完成)
│
├── handler
│   └── GlobalExceptionHandler.java (✅ 已完成)
│
├── interceptor
│   ├── wx
│   │   └── WxAuthInterceptor.java (⚠️ 待补充)
│   └── admin
│       └── AdminAuthInterceptor.java (⚠️ 待补充)
│
├── repository
│   ├── wx
│   │   └── WxUserRepository.java (⚠️ 待补充 findByOpenid)
│   ├── admin
│   │   └── AdminRepository.java  (✅ 已完成)
│   ├── product
│   │   └── ProductRepository.java (✅ 已完成)
│   └── order
│       ├── OrderRepository.java  (📁 待开发)
│       └── OrderItemRepository.java (📁 待开发)
│
├── resolver
│   ├── CurrentUserArgumentResolver.java   (⚠️ 待补充)
│   └── CurrentAdminArgumentResolver.java  (⚠️ 待补充)
│
├── service
│   ├── wx
│   │   ├── WxUserService.java    (⚠️ 待补充)
│   │   └── WxAuthService.java    (⚠️ 待重构)
│   ├── admin
│   │   ├── AdminService.java     (📁 待补充)
│   │   └── AdminAuthService.java (✅ 已完成)
│   ├── product
│   │   └── ProductService.java   (✅ 已完成)
│   └── order
│       └── OrderService.java     (📁 待开发)
│
└── util
    ├── SnowflakeIdWorker.java    (✅ 已完成)
    ├── wx
    │   ├── WxJwtUtil.java        (⚠️ 待重构)
    │   └── WxDecryptUtil.java    (⚠️ 待补充)
    └── admin
        └── AdminJwtUtil.java     (✅ 已完成)
环境要求
JDK 17+

Maven 3.6+

MySQL 8.0+

Redis（可选，用于缓存和购物车，待补充）

快速开始
1. 克隆项目
bash
git clone https://github.com/Y-hh/MKappshop.git
cd MKappshop
2. 创建数据库
sql
CREATE DATABASE mall_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
3. 修改配置文件
复制 application.yml.template 为 application.yml（或直接修改），填写正确的数据库用户名密码：

yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mall_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
4. 启动项目
在 IDEA 中运行 MKappshopApplication.java，或在项目根目录执行：

bash
mvn spring-boot:run
5. 测试接口
商品列表：GET http://localhost:8080/api/products

管理员登录：POST http://localhost:8080/api/admin/auth/login

注意事项
雪花ID：微信用户、商品、订单等实体使用雪花算法生成主键，需确保 workerId 和 dataCenterId 配置正确（目前硬编码在 SnowflakeIdWorker 中，建议改为配置文件注入）。

JWT密钥：管理员和微信用户的 JWT 密钥暂未分离，建议重构时分别配置。

跨域：开发阶段已在 SecurityConfig 中允许所有跨域，生产环境需收紧。

表名关键字：order 表名使用了反引号转义，也可考虑改名为 orders 避免关键字。

待办事项
完成微信用户认证重构（WxAuthService、WxJwtUtil）

补充拦截器 (WxAuthInterceptor, AdminAuthInterceptor) 及参数解析器

开发购物车模块

开发订单模块（下单、支付对接）

集成 Redis 缓存

完善管理员后台功能

提示：目录中标记 ⚠️ 和 📁 的为待完善或待开发部分，欢迎贡献代码。
