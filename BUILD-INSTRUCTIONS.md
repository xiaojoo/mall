# Mall 项目构建说明

## 快速开始

### 方式一：跳过测试打包（推荐，最快）
```bash
# Windows
mvn.cmd clean package -DskipTests

# Linux/Mac
mvn clean package -DskipTests
```

### 方式二：完整构建（包含测试）
需要先配置好 Nacos、MySQL、Redis、RabbitMQ 等环境。

## 常见问题

### 1. 测试失败：No spring.config.import set
**错误信息：**
```
APPLICATION FAILED TO START
Description: No spring.config.import property has been defined
Action: Add a spring.config.import=nacos: property to your configuration.
```

**原因：** 测试时需要加载 Nacos 配置，但本地没有配置 Nacos。

**解决方案：**
- **方案 A（推荐）**：跳过测试打包 `mvn clean package -DskipTests`
- **方案 B**：在测试配置中添加 `spring.config.import=optional:nacos:`

### 2. 编译警告：已过时的类
以下警告是正常的，不影响功能：
- `GenericJackson2JsonRedisSerializer` - 已使用正确的构造函数
- `Jackson2JsonMessageConverter` - 已使用正确的构造函数

这些类被 Spring 标记为过时，但目前是官方推荐的实现方式，只需使用正确的构造函数即可。

## 构建命令

### 只编译
```bash
mvn clean compile
```

### 编译并打包（跳过测试）
```bash
mvn clean package -DskipTests
```

### 编译并运行所有测试
```bash
mvn clean test
```

### 安装到本地 Maven 仓库
```bash
mvn clean install -DskipTests
```

### 构建指定模块
```bash
mvn clean package -pl mall-member -am -DskipTests
```

## IDEA 配置

### 设置跳过测试
1. 打开 IDEA 的 Maven 面板
2. 点击 **Toggle Skip Tests Mode** 按钮（禁止符号图标）
3. 执行 `package` 或其他目标

### 配置 Run/Debug Configuration
如果需要在 IDEA 中运行测试：
1. Run/Debug Configurations
2. 添加 `spring.config.import=optional:nacos:` 到 Environment variables
3. 或者添加 `--spring.config.import=optional:nacos:` 到 Program arguments

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| mall-gateway | 88 | 网关服务 |
| mall-member | 8000 | 会员服务 |
| mall-product | 8001 | 商品服务 |
| mall-order | 8002 | 订单服务 |
| mall-ware | 8003 | 仓储服务 |
| mall-coupon | 8004 | 优惠券服务 |
| mall-auth | 8005 | 认证服务 |
| mall-cart | 8006 | 购物车服务 |
| mall-search | 8007 | 搜索服务 |
| mall-seckill | 8008 | 秒杀服务 |
| mall-third-party | 8009 | 第三方服务 |
| mall-admin | - | 管理后台 |

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+
- Nacos 2.0+（可选，用于配置中心）
- RabbitMQ 3.8+（可选，用于消息队列）
- Elasticsearch 7.x（可选，用于搜索）

## 下一步

1. 配置 Nacos 配置中心（参考 `nacos-config/README.md`）
2. 初始化数据库（参考各服务的 SQL 脚本）
3. 修改配置文件中的数据库、Redis 等连接信息
4. 启动 Nacos、MySQL、Redis 等服务
5. 启动各个微服务
