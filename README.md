# Mall 微服务商城

基于 Spring Boot 4 + Spring Cloud 的分布式电商后端系统，采用 Nacos 作为注册中心与配置中心。

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 4.0.7 / Spring Cloud 2025.1.2 |
| 微服务组件 | Nacos 3.0.3（注册/配置）、Sentinel 1.8.9（限流降级）、Spring Cloud Gateway（Spring Cloud Alibaba 2025.1.0.0） |
| 持久层 | MyBatis-Plus 3.5.16、Druid |
| 数据库 | MySQL 8（mysql-connector 9.2） |
| 缓存 | Redis、Redisson、Caffeine |
| 搜索 | Elasticsearch 8.15 |
| 存储 | 阿里云 OSS / 七牛云 / 腾讯 COS |
| 文档 | SpringDoc OpenAPI 3.1.0 |
| 工具库 | Hutool、Fastjson2、Guava、Commons |
| JDK | Java 21 |

## 📦 微服务模块

| 模块 | 端口 | 说明 |
|------|------|------|
| `mall-gateway` | 88 | API 网关，路由转发、鉴权 |
| `mall-auth` | 10011 | 认证授权服务（OAuth2 / 登录） |
| `mall-admin` | 10012 | 后台管理服务（原 mall-fast） |
| `mall-product` | 10013 | 商品服务（SPU/SKU、分类、品牌） |
| `mall-member` | 10014 | 会员服务（用户、等级、积分） |
| `mall-order` | 10015 | 订单服务 |
| `mall-cart` | 10016 | 购物车服务 |
| `mall-ware` | 10017 | 仓储服务（库存管理） |
| `mall-coupon` | 10018 | 优惠/优惠券服务 |
| `mall-search` | 10019 | 搜索服务（ES） |
| `mall-seckill` | 10020 | 秒杀服务 |
| `mall-weapp` | 10021 | 微信小程序服务（API 网关适配层） |
| `mall-third-party` | 10022 | 第三方服务（OSS、短信等） |
| `mall-common` | — | 公共模块（工具类、通用实体） |

### 🚪 最终端口方案

| 端口 | 模块 | 端口 | 模块 |
|------|------|------|------|
| 88 | mall-gateway（不变） | 10017 | mall-ware |
| 10011 | mall-auth | 10018 | mall-coupon |
| 10012 | mall-admin | 10019 | mall-search |
| 10013 | mall-product | 10020 | mall-seckill |
| 10014 | mall-member | 10021 | mall-weapp |
| 10015 | mall-order | 10022 | mall-third-party |
| 10016 | mall-cart | | |

## 📋 环境要求

- **JDK** 21+
- **Maven** 3.8+
- **MySQL** 8.0+
- **Redis** 6.0+
- **Nacos** 3.x（注册中心 + 配置中心）
- **Elasticsearch** 8.x（可选，搜索服务需要）

## 🚀 本地开发

### 1. 克隆项目

```bash
git clone <repo-url>
cd mall
```

### 2. 初始化数据库

`db/` 目录下包含各业务库的 SQL 脚本（一个库一个文件）。先执行 `database.sql` 创建全部数据库，再逐个导入（注意：`mall_*.sql` 内**没有 `USE` 语句**，需把库名作为参数）。

**方式 A：MySQL 跑在容器里（`install-infra.sh` 安装的，推荐）**
```bash
# 创建全部数据库
docker exec -i mysql mysql -uroot -p<密码> < db/database.sql

# 逐个导入（库名作为参数）
docker exec -i mysql mysql -uroot -p<密码> mall_auth < db/mall_auth.sql   # 权限库（后台）
docker exec -i mysql mysql -uroot -p<密码> mall_pms  < db/mall_pms.sql    # 商品库
docker exec -i mysql mysql -uroot -p<密码> mall_ums  < db/mall_ums.sql    # 会员库
docker exec -i mysql mysql -uroot -p<密码> mall_oms  < db/mall_oms.sql    # 订单库
docker exec -i mysql mysql -uroot -p<密码> mall_sms  < db/mall_sms.sql    # 营销库（含轮播/跑马灯/大促种子）
docker exec -i mysql mysql -uroot -p<密码> mall_wms  < db/mall_wms.sql    # 仓储库
# mall_admin.sql（旧 renren 后台，已废弃）内含 USE mall_admin;，无需传库名
```

**方式 B：宿主机已装 MySQL 客户端**
```bash
mysql -h127.0.0.1 -uroot -p < db/database.sql
mysql -h127.0.0.1 -uroot -p mall_auth < db/mall_auth.sql
mysql -h127.0.0.1 -uroot -p mall_pms  < db/mall_pms.sql
mysql -h127.0.0.1 -uroot -p mall_ums  < db/mall_ums.sql
mysql -h127.0.0.1 -uroot -p mall_oms  < db/mall_oms.sql
mysql -h127.0.0.1 -uroot -p mall_sms  < db/mall_sms.sql
mysql -h127.0.0.1 -uroot -p mall_wms  < db/mall_wms.sql
```

验证：`docker exec -i mysql mysql -uroot -p<密码> -e "show databases;"` 应看到 `mall_auth/pms/ums/oms/sms/wms`；`select user_id,username,status from mall_auth.sys_user;` 应有 `admin` 超管。

### 3. 启动基础设施

确保 Nacos、MySQL、Redis 已启动运行。

### 4. Nacos 配置

在各服务的 Nacos 配置中配置数据源、Redis 等连接信息，参考 `application.yml` 中的 `${}` 占位符。

### 5. 修改配置

修改各模块 `src/main/resources/application.yml` 中的 Nacos 地址：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

### 6. 编译 & 启动

```bash
# 全局编译（跳过测试）
mvn clean install -DskipTests

# 按顺序启动服务（Gateway 最先）
# 1. mall-gateway
# 2. mall-auth
# 3. mall-product / mall-member / mall-order / mall-coupon / mall-ware
# 4. mall-search / mall-seckill / mall-cart / mall-third-party
# 5. mall-admin（后台管理）
```

每个模块可独立启动：

```bash
cd mall-product
mvn spring-boot:run
```

## 🐳 环境部署（Docker 一键脚本）

> 适用于 **Linux + Docker** 单机部署。根目录两个脚本，**可重复执行**（会自动先删除同名容器再启动）。

### 安装脚本

| 脚本 | 安装内容 |
|---|---|
| `install-infra-basic.sh` | Redis、MinIO、Nginx（反代到网关） |
| `install-infra.sh` | Elasticsearch(7.17 + IK 分词器)、Kibana、RabbitMQ、Zipkin、MySQL、Nacos、Sentinel，并自动执行 ES 商品索引重建 `mall-search/src/main/resources/create-product-index.sh` |

### 前提

- Linux 主机，已安装 **Docker**。
- 免 sudo 使用 Docker（加入 docker 组，一次即可）：
  ```bash
  sudo usermod -aG docker $USER && newgrp docker
  ```

### 使用

```bash
# 1) 基础组件（Redis / MinIO / Nginx）
bash install-infra-basic.sh

# 2) 主环境（ES+IK / Kibana / RabbitMQ / Zipkin / MySQL / Nacos / Sentinel + 自动建 ES 索引）
bash install-infra.sh
```

数据/配置默认存 `~/mall-data`（由 `DATA_ROOT` 控制），可用环境变量覆盖：

```bash
# 指定数据目录
DATA_ROOT=/data/mall bash install-infra.sh

# 单点覆盖（地址、版本、密码等）
ES_HOST=127.0.0.1 MYSQL_ROOT_PASSWORD=xxx bash install-infra.sh
```

### 安装后手动步骤

1. **导入数据库**（MySQL 在容器里，用 `docker exec -i`；`mall_*.sql` 无 `USE`，需传库名）：
   ```bash
   docker exec -i mysql mysql -uroot -padmin123 < db/database.sql         # 先建库
   docker exec -i mysql mysql -uroot -padmin123 mall_auth < db/mall_auth.sql
   docker exec -i mysql mysql -uroot -padmin123 mall_pms  < db/mall_pms.sql
   docker exec -i mysql mysql -uroot -padmin123 mall_ums  < db/mall_ums.sql
   docker exec -i mysql mysql -uroot -padmin123 mall_oms  < db/mall_oms.sql
   docker exec -i mysql mysql -uroot -padmin123 mall_sms  < db/mall_sms.sql
   docker exec -i mysql mysql -uroot -padmin123 mall_wms  < db/mall_wms.sql
   ```
   （`<密码>` 换成 `MYSQL_ROOT_PASSWORD`，默认 `admin123`）
2. **导入 Nacos 配置**：登录 `http://<server-ip>:8080/nacos`（Nacos 3.x 控制台在 8080；8848 是 API），账号 `nacos/nacos`，把 `nacos-config/*.yaml` 逐个导入（**data-id 即文件名**）。
3. **对齐密钥/密码**：把各服务 `spring.datasource.password` / `spring.data.redis.password` 设为与脚本一致（默认 `admin123`）；填好 `nacos-config/*.yaml` 里的 `CHANGE_ME_*` 占位（微博/微信/支付宝/OSS/MinIO 密钥等）。

> 提示：`install-infra.sh` 末尾会把业务配置里的 `${SERVER_IP:localhost}` 占位符批量替换为探测到的本机 IP；若希望服务容器用环境变量解析，可在容器启动时传 `-e SERVER_IP=<ip>`。数据目录默认 `/data/mall`（`DATA_ROOT` 可覆盖）；ES/Mysql 数据目录权限异常时可分别 `chown -R 1000:1000 /data/mall/es`、`chown -R 999:999 /data/mall/mysql/data`。

## 🤖 服务部署（Jenkins / Docker 流水线）

> 方式一：用仓库自带 **`Jenkinsfile.docker`**（每个模块单独构建/部署）。前提：Jenkins 节点**已装 docker**（无需装 Maven/JDK，构建用 Maven 容器）。

### 1. Jenkins 流水线配置
- 新建 Pipeline，**脚本路径**填 `Jenkinsfile.docker`。
- 参数：
  | 参数 | 说明 |
  |---|---|
  | `PROJECT_NAME` | 本次部署的模块（一次一个，实现"每个模块单独部署"） |
  | `IMAGE_TAG` | 镜像标签（默认 `latest`） |
  | `DEPLOY_MODE` | `docker` / `k8s`（可选） |
  | `USE_NGINX` | 是否由 nginx 统一入口（默认 true；若不想 Jenkins 碰宿主 nginx 可关掉） |
  | `DEPLOY` | 是否实际部署 |

### 2. 镜像仓库
`Jenkinsfile.docker` 顶部 `REGISTRY`/`NAMESPACE` 默认：
```groovy
REGISTRY = '127.0.0.1:5000'   // 本机私有仓库 registry:2
NAMESPACE = 'mall'
```
- 本地仓库 `registry:2` 默认无鉴权，直接 `docker build/push`即可（脚本已去掉强制 login）。
- 若换其它仓库（阿里云/Docker Hub/Harbor），改 `REGISTRY`/`NAMESPACE`，并如需要鉴权，把推送那步改回 `withCredentials` + `docker login`。
- ⚠️ 本地仓库是 http：docker 主机需在 `/etc/docker/daemon.json` 加 `"insecure-registries": ["127.0.0.1:5000"]` 并重启 docker。

### 3. 构建
`Jenkinsfile.docker` 用 **Maven 容器**构建（节点无需装 maven）：
```groovy
docker run --rm -v "$PWD":/usr/src -v "$HOME/.m2":/root/.m2 -w /usr/src maven:3.9-eclipse-temurin-21 mvn -Dmaven.test.skip=true -gs /usr/src/mvn-settings.xml clean package -pl <模块> -am
```
模块 `Dockerfile` 基镜像为 `eclipse-temurin:21-jre`（不要用 `21-jre-alpine`，该 tag 不存在）。

### 4. 部署 + nginx（用 mall 的 nginx 容器，免 sudo、不碰其它项目 nginx）
- `deployByDocker()`：`docker run -d --name <模块> --network mall-net --restart unless-stopped -e SPRING_PROFILES_ACTIVE=prod -p <端口>:<端口> <镜像>`。
- `USE_NGINX=true` 时 `configureNginx()` 写 **`/data/mall/nginx/conf.d/mall.conf`**（`install-infra-basic.sh` 起的 **mall nginx 容器**挂载目录），再 `docker exec nginx nginx -t && docker exec nginx nginx -s reload`——**不需要 sudo**，也不改动别的项目（如 `llmops-nginx` 的 80 端口）。
  ```
  server {
      listen       8088;                     # 避开已被占用的 80
      root   /data/mall/web;                 # 前端静态目录
      location /      { try_files $uri $uri/ /index.html; }   # SPA 回退
      location /api/  { proxy_pass http://127.0.0.1:88; }     # 转发到网关(88)
  }
  ```
- 仓库地址：`http://<server-ip>:8088/`（前端）、`http://<server-ip>:8088/api/**`（网关→服务）。
- 若宿主机没有 `/etc/nginx`（nginx 是别的项目/容器的），**务必**用上面容器版 nginx，别写 `/etc/nginx`（否则报 `No such file or directory`）。
- 若不想 Jenkins 配 nginx，把 `USE_NGINX` 关掉，只部署容器。

### 5. 前端部署
- 先保证有 mall 的 nginx 容器：`bash install-infra-basic.sh`（会创建 `nginx` 容器，监听 **8088**）。
- 把前端构建产物（如 `vite/dist` 的 `index.html` + 静态资源）放到 **`/data/mall/web`**（`NGINX_FRONTEND_ROOT`），nginx 即服务前端并转发 `/api/**` 到网关。

## 📁 目录结构

```
mall/
├── db/                    # 数据库脚本
├── mall-common/           # 公共模块（工具类、通用 DTO/VO、异常处理）
├── mall-gateway/          # API 网关
├── mall-auth/             # 认证服务
├── mall-admin/            # 后台管理服务
├── mall-product/          # 商品服务
├── mall-member/           # 会员服务
├── mall-order/            # 订单服务
├── mall-cart/             # 购物车服务
├── mall-ware/             # 仓储服务
├── mall-coupon/           # 优惠券服务
├── mall-search/           # 搜索服务
├── mall-seckill/          # 秒杀服务
├── mall-third-party/      # 第三方服务
├── mall-weapp/            # 微信小程序后端服务
├── pom.xml                # 父 POM（依赖版本管理）
├── Jenkinsfile            # CI/CD 流水线
└── ingress/               # Kubernetes Ingress 配置
```

## 📖 API 文档

服务启动后访问 SpringDoc 提供的 Swagger UI：

- 网关路由后访问：`http://localhost:88/<服务名>/doc.html` 或 `/swagger-ui.html`
- 各服务独立访问：`http://localhost:<端口>/swagger-ui.html`

## 🚢 部署

项目支持两套部署方式：

**1. Kubernetes 部署（原方案）**：参见 `Jenkinsfile` 和 `ingress/` 目录。

```bash
# 构建镜像并推送（参考 Jenkinsfile）
mvn clean package -DskipTests
docker build -t mall/<service-name>:latest .
```

**2. Docker Compose 部署（推荐单机/中小规模）**：参见 [mall-deploy](../mall-deploy) 目录
（独立部署仓库），提供 5 个 Jenkinsfile：

| Jenkinsfile | 作用 | 是否可重复执行 |
|-------------|------|----------------|
| `Jenkinsfile.infra` | 基础设施初始化（MySQL/Redis/RabbitMQ/ES/MinIO/Nacos） | 一次性 |
| `Jenkinsfile.db` | 数据库初始化/升级（版本追踪，幂等） | ✅ 可重复 |
| `Jenkinsfile.backend` | 后端微服务构建+部署 | ✅ 可重复 |
| `Jenkinsfile.nginx` | Nginx 反代部署 | ✅ 可重复 |
| `Jenkinsfile.frontend` | 前端构建+部署（mall-web/mall-ui） | ✅ 可重复 |

> 注意：仓库各模块自带的 `Dockerfile` 是旧 JDK8 镜像，与当前 Java 21 代码不匹配；
> Docker 部署请统一使用 mall-deploy 中的 `backend.Dockerfile`（JDK 21）。

## 📱 前端项目

| 仓库 | 说明 |
|------|------|
| [mall-weapp-mini](https://gitee.com/xiaono/mall-weapp-mini) | 微信小程序前端（独立仓库） |
| `mall-weapp/` | 微信小程序后端服务（本仓库内） |

## 📋 安全与架构审计记录（2026-09-03）

> 本节为**只读分析记录**（不自动修复）。基于代码 + 配置的实证排查，涉及四个方向：
> ① Shiro→Spring Security 迁移、② mall-auth 与 mall-admin 路由/表归属、③ 安全配置审计、④ 编译验证。
>
> **开源脱敏说明（已执行）**：为开源已将源码中的个人邮箱/姓名、真实密钥、内网 IP、私有域名替换为占位符。
> - 占位约定：`localhost` 表示**本地/本机**；`example.com` 表示示例域名；`CHANGE_ME_*` / `YOUR_*` / `CHANGE_ME` 表示**需自行填写**的密钥或密码占位。
> - 默认后台账号：`admin / admin123`（公共占位密码，部署后请立即修改）。
> - ⚠️ 仍建议：git 历史中残留作者邮箱与旧密钥，公开前请用全新历史或 `git filter-repo` 重写；相关平台侧密钥请作废/轮换。

### ① Shiro → Spring Security 迁移：已完成

- 全库（含所有 `pom.xml`、`.java`）检索 `shiro` / `org.apache.shiro`：**0 处实际引用**（仅 `UPGRADE_LOG.md` 作为历史记录）。
- 文档标注含 Shiro 的 `com.mall.member.sys.*` 包（`ShiroUtils`/`JWTFilter`/`JWTRealm`/`ShiroService`/`ShiroServiceImpl`/`AbstractController`）**已整体删除**。
- 权限代码已迁至 **`mall-auth` 的 `perm` 包**，为**自定义 `@RequirePermission` + `PermissionInterceptor` + JJWT** 方案，**非 Spring Security 框架**；仅使用 `spring-security-crypto`(BCrypt)、`spring-security-core`(AccessDeniedException)。
- `.m2` 中残留 `shiro-*` jar 为旧项目无引用缓存，无害，可清理。
- **结论**：Shiro 迁移在代码层面已闭环；`UPGRADE_LOG.md`/`docs/` 相关表述已过期。无修复动作。

### ② mall-auth 与 mall-admin 路由/表归属：无直接路由冲突，但有真实隐患

**路由归属**（`nacos-config/mall-gateway.yaml`）：

| 路径 | 后端 | 说明 |
|---|---|---|
| `/api/sys/schedule/**`、`/api/sys/scheduleLog/**` | mall-admin | 定时任务未迁移；路由在前，避免被吞（正确） |
| `/api/sys/**`（其余全部） | mall-auth | RBAC + JWT 主系统 |
| `/api/member|product|order|cart|search|seckill|auth|sms/**` | 各业务服务 | 透传 |
| `/api/ware|coupon|weapp/**` | 各业务服务 | StripPrefix |
| `/api/thirdparty/**`、`/api/file/**` | mall-third-party | RewritePath |

- 路由优先级正确（`mall-admin-schedule` 位于 `mall-auth-sys` 之前），`/api/sys/**` 已从旧 mall-admin 切到 mall-auth。

**表归属**：

- `mall-auth` → `mall_auth` 库（RBAC 六表）。`mall-admin` → `mall_admin` 库（renren 旧 schema）。两库同名表但库不同，**无 DB 冲突**，仅命名混淆。
- `db/mall_admin.sql` 为清理脚本：A 组删（`sys_user/role/menu/user_role/role_menu/log`）、B 组删（`sys_user_token/sys_captcha/sys_config/sys_oss/tb_user/QRTZ_×11`）、C 组保留（`schedule_job/schedule_job_log`）。

**🔴 真实隐患**：

1. **`mall-admin` 的 `AdminAuthInterceptor` 为空壳**：只判断 `token != null`，**从不校验**（`SysUserTokenService` 注入未使用，注释"为简单、生产用 Redis"），随后无条件 `return true`。→ 访问 mall-admin 受拦路径（含 `/sys/schedule/**`）任意 `token: xxxx` 即通过，形同未鉴权。
2. **鉴权体系脱节**：mall-admin 用自己的 `/sys/login` 签发 renren 式 token（`sys_user_token` 表，已被列为删除），但网关把 `/api/sys/login`、`/api/sys/**` 全部发给 mall-auth。当前前端任务调度页可用，**仅因空壳拦截器放行一切**；若补全 mall-admin 校验，前端携带的 mall-auth JWT 将无法被其校验，任务调度页立即 401。
3. **僵尸控制器**：mall-admin 的 `SysUser/SysRole/SysMenu/SysLog/SysConfig/SysOssController` 仍存在，但其后端表已被列为删除，直接访问会因表缺失报错；仅 `SysScheduleJobController(-Log)` 在服役。
4. **路由死区**：`/api/sys/config/**`、`/api/sys/oss/**` 落在 mall-auth，但 mall-auth 无对应控制器 → 404。`db/mall_admin.sql` 注释确认 mall-web 已不调用 `/sys/config`、上传走 `/thirdparty/oss`，**当前无害**。

### ③ 安全配置审计

**🔴 高危：真实密钥已提交进仓库**

| 密钥 | 位置 |
|---|---|
| 微博 OAuth `client_secret` | `mall-auth/.../OAuth2Controller.java:31`（含 `client_id`、`redirect_uri` 硬编码） |
| 微信 MP AppSecret | `mall-third-party/src/main/resources/application.yml:34`、`application-prod.yml`、`nacos-config/mall-third-party.yaml:67` |
| 阿里云 OSS `secret-key` | `nacos-config/mall-third-party.yaml:28`、`application.yml:12`、`application-prod.yml:9` |
| MinIO 默认弱口令 | `application.properties`(`CHANGE_ME_MINIO`)、nacos(`accessKey/secretKey → CHANGE_ME`) |

**🟠 中危：JWT 默认 secret（有覆盖机制，但仓库值为占位）**

- `auth.jwt.secret = YOUR_AUTH_JWT_SECRET_AT_LEAST_32_BYTES`（`JwtProperties` 默认值 + `mall-auth/application.yml` + `nacos-config/mall-auth.yaml`）
- `member.jwt.secret = YOUR_MEMBER_JWT_SECRET_AT_LEAST_32_BYTES`（`MemberJwtUtils` 默认值，并复制到 5 处 `nacos-config/mall-{auth,order,cart,member,third-party}.yaml` 及各 `application.yml`，易漂移）
- 若 Nacos 未覆盖，任何读到仓库的人可伪造管理员/会员 token。

**🟢 中低危：鉴权边界与潜在越权点**

- `PermissionInterceptor` **fail-open**：`/sys/**` 下未标 `@RequirePermission` 的方法，仅需合法 JWT 即可访问，不校验具体权限。已排查：`/sys/role/select`、`/sys/menu/select` 会向任意登录用户泄露完整角色/菜单（含权限码）；`/sys/user/updatePassword|profile|perms`、`/sys/log/stats`、`/sys/menu/nav` 为自服务类，设计合理。
- ✅ 安全特性：`SysUserController.updatePassword/profile` 以 JWT 解析的 `userId` 为准、忽略 body，防越权改他人。
- 文档/代码不一致：`@RequirePermission#logical` 默认 **AND**（代码） vs 文档称默认 OR（AND 更严，实际更安全）。
- 超管旁路：`userId==1L` 或含 `*:*:*`（硬编码，预期设计）。
- `mall-auth` 网关无 `/api/app/**` 路由 → `app` 包（`AppRegisterController`/`AppTestController`/`AuthorizationInterceptor`/`JwtUtils`）**不对外暴露**；且 `JwtUtils` 的 `renren.jwt.*` 在**任何配置中都未定义**（secret 回退硬编码、`expire` 默认 0、`header` 为 null）——遗留/死代码。
- 会员端：`LoginUserInterceptor`(order) 对 `/api/order/**` **fail-closed**（无/无效 JWT → 401），对管理端 `/api/order/order/**`、`/api/order/orderreturnapply/**` **显式放行**（admin JWT 绕过会员校验），需确认这些接口有上游权限校验。

### ④ 编译验证 `mvn clean install`

- **本环境无法完成**（非代码问题）：沙箱**无外网**（Maven Central、阿里云镜像均 SSL 失败），且本地 `.m2` 缺少精确版本 —— 需 `spring-cloud-dependencies:2025.1.2` 与 `spring-cloud-alibaba-dependencies:2025.1.0.0`，本地仅 `2025.1.0`/`2025.0.0.0`。
- 构建在**父 POM 依赖解析阶段**即失败（`Non-resolvable import POM ... .part.lock`），未进入模块编译，**无代码级编译错误信息**。
- 旁证：14 个模块 `target/` 均存在**先前成功编译产物**（`*.jar` + compiled `*.class`），说明有网环境曾通过构建。
- 建议：在有网环境或补齐上述版本后执行 `mvn clean install -DskipTests`。

### 汇总优先级

| 优先级 | 问题 | 涉及 |
|---|---|---|
| P0 安全漏洞 | mall-admin 空壳拦截器形同未鉴权（schedule 接口） | mall-admin |
| P0 密钥泄露 | 微博/微信/OSS/MinIO 密钥入库 | mall-auth / mall-third-party |
| P1 密钥默认值 | `auth.jwt.secret`/`member.jwt.secret` 占位值需生产覆盖、去重 | mall-auth / mall-common |
| P1 遗留清理 | mall-admin 僵尸控制器 + `/api/sys/config|oss` 路由死区 + `renren.jwt`/`/app/**` 死代码 | mall-admin / mall-auth / gateway |
| P2 越权细化 | `/sys/role/select`、`/sys/menu/select` 信息泄露 | mall-auth |
| P2 文档修正 | `@RequirePermission` 默认 logical、Shiro 状态表述 | docs |

---

## License

[CC0 1.0 Universal](LICENSE)（公有领域奉献：可自由使用、修改、分发及商用，无需署名、无其它限制）
