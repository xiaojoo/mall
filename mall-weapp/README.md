# mall-weapp

mall 微服务商城微信小程序后端服务模块。

## 说明

本模块为微信小程序提供后端 API 接口，作为轻量级适配层，通过 OpenFeign 调用 mall 各微服务。

对应的前端项目：[mall-weapp-mini](https://gitee.com/xiaono/mall-weapp-mini)

## 模块结构

```
mall-weapp/
├── src/main/java/com/mall/weapp/
│   ├── MallWeappApplication.java     # 启动类
│   ├── app/                          # Controller 层
│   │   ├── WeappHomeController.java  # 首页接口
│   │   ├── WeappProductController.java # 商品接口
│   │   ├── WeappCartController.java  # 购物车接口
│   │   ├── WeappOrderController.java # 订单接口
│   │   └── WeappMemberController.java # 会员接口
│   ├── entity/                       # 实体类
│   ├── feign/                        # OpenFeign 远程调用
│   │   ├── CartFeignService.java
│   │   ├── MemberFeignService.java
│   │   ├── OrderFeignService.java
│   │   ├── ProductFeignService.java
│   │   └── fallback/                # 熔断降级
│   └── resources/
│       └── application.yml           # 服务配置
└── pom.xml
```

## API 接口

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/weapp/home/index` | GET | 首页数据 |
| `/api/weapp/product/list` | GET | 商品列表 |
| `/api/weapp/product/info/{id}` | GET | 商品详情 |
| `/api/weapp/product/category` | GET | 分类树 |
| `/api/weapp/cart/list` | GET | 购物车列表 |
| `/api/weapp/cart/add` | POST | 加入购物车 |
| `/api/weapp/order/list` | GET | 订单列表 |
| `/api/weapp/order/create` | POST | 创建订单 |
| `/api/weapp/member/info` | GET | 用户信息 |
| `/api/weapp/member/login` | POST | 登录 |

## 启动

```bash
cd mall-weapp
mvn spring-boot:run
```

依赖服务需先启动：Nacos、mall-gateway、mall-product、mall-cart、mall-order、mall-member。
