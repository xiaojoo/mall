# Nacos 配置说明

## 配置列表

| 服务名 | Data ID | 端口 | 说明 |
|--------|---------|------|------|
| mall-member | mall-member.yaml | 8000 | 会员服务 |
| mall-product | mall-product.yaml | 8001 | 商品服务 |
| mall-order | mall-order.yaml | 8002 | 订单服务 |
| mall-ware | mall-ware.yaml | 8003 | 仓储服务 |
| mall-coupon | mall-coupon.yaml | 8004 | 优惠券服务 |
| mall-auth | mall-auth.yaml | 8005 | 认证服务 |
| mall-gateway | mall-gateway.yaml | 88 | 网关服务 |
| mall-cart | mall-cart.yaml | 8006 | 购物车服务 |
| mall-search | mall-search.yaml | 8007 | 搜索服务 |
| mall-seckill | mall-seckill.yaml | 8008 | 秒杀服务 |
| mall-third-party | mall-third-party.yaml | 8009 | 第三方服务 |

## 使用步骤

### 1. 创建 Nacos 配置

登录 Nacos 控制台（通常是 `http://nacos-host:8848/nacos`），然后：

1. 进入 **配置管理** -> **配置列表**
2. 点击 **+** 号创建新配置
3. 填写以下信息：
   - **Data ID**: `mall-member.yaml`（根据服务名替换）
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**: 复制对应 yaml 文件的内容

### 2. 修改配置

根据实际情况修改以下配置项：

#### 数据库配置
```yaml
spring:
  datasource:
    username: root
    password: 你的数据库密码
    url: jdbc:mysql://你的 MySQL 地址：3306/mall_ums?...
```

#### Redis 配置
```yaml
spring:
  redis:
    host: 你的 Redis 地址
    port: 6379
    password: 你的 Redis 密码
```

#### Elasticsearch 配置（search 服务）
```yaml
spring:
  elasticsearch:
    rest:
      uris: http://你的 ES 地址:9200
      username: elastic
      password: 你的 ES 密码
```

#### RabbitMQ 配置（seckill 服务）
```yaml
spring:
  rabbitmq:
    host: 你的 RabbitMQ 地址
    port: 5672
    username: guest
    password: guest
```

#### OSS 配置（third-party 服务）
```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    bucket: 你的 Bucket 名称
    access-key: 你的 AccessKey
    secret-key: 你的 SecretKey
```

### 3. 启动服务

确保 bootstrap.properties 中配置了 Nacos 地址：

```properties
spring.application.name=mall-member
spring.cloud.nacos.config.server-addr=你的 nacos 地址：8848
```

## 配置说明

### 通用配置项
- `spring.datasource`: 数据库连接配置
- `spring.redis`: Redis 连接配置
- `mybatis-plus`: MyBatis Plus 配置
- `server.port`: 服务端口
- `logging.level`: 日志级别

### 服务端口分配
- 8000: mall-member
- 8001: mall-product
- 8002: mall-order
- 8003: mall-ware
- 8004: mall-coupon
- 8005: mall-auth
- 8006: mall-cart
- 8007: mall-search
- 8008: mall-seckill
- 8009: mall-third-party
- 88: mall-gateway

## 注意事项

1. **数据库命名**: 每个服务对应不同的数据库
   - mall_ums: 会员系统
   - mall_pms: 商品系统
   - mall_oms: 订单系统
   - mall_wms: 仓储系统
   - mall_sms: 营销系统（优惠券、秒杀）

2. **Redis 隔离**: 建议使用不同的 database 或 key 前缀隔离各服务

3. **配置刷新**: 修改 Nacos 配置后，服务会自动刷新（需要 `@RefreshScope` 注解）

4. **配置优先级**: Nacos 配置 > application.yml > bootstrap.properties
