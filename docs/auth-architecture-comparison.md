# 权限认证架构演进与对比（Session → JWT 混合）

> 仓库：mall（mall-auth 权限服务 + mall-web/mall-ui 前端）
> 本文对比三代认证架构，重点说明 **release_20260819 的 JWT 双 token + 黑名单混合方案**（当前维护架构）
> 配套文档：`docs/permission-system.md`（RBAC 权限模型与使用指南，不含认证载体对比）
> 最后更新：2026-08-18

---

## 1. 演进路线（三代架构）

| 代际 | 时期 | 载体 | 说明 |
|---|---|---|---|
| 第一代 | 早期 | renren-fast（mall-admin + `mall_admin` 库） | 模板页跳转登录，`sys_user_token` 表存 UUID token，`QRTZ_*` 调度表 |
| 第二代 | release_20260818 | mall-auth **HttpSession** + UUID token | 纯 API 化重构后重建权限系统；`spring-session-data-redis` 共享会话；前端 token header 是装饰性的，实际靠 session cookie |
| 第三代 | **release_20260819（当前）** | mall-auth **JWT 双 token + 黑名单**（管理端）+ **会员 JWT**（C 端） | 无状态 access token（30min）+ 可吊销 refresh token（7d）+ Redis 黑名单；C 端微博/微信/账号密码登录统一签发会员 JWT |

---

## 2. 当前架构（第三代，release_20260819）

### 2.1 管理端（mall-web → 网关 → mall-auth）

```
POST /sys/login（图形验证码 + BCrypt 密码校验）
        │
        ▼
JwtTokenService.issueTokens(userId, username)
        ├─ access token  (30min, claim: jti/type=access/username)
        ├─ refresh token (7d,   claim: jti/type=refresh)
        └─ Redis: auth:refresh:{userId} = refreshJti（TTL 7d）
        │
        ▼
返回 { code:0, token, refreshToken, expiresIn }
```

**请求鉴权链路（每个 /sys/** 请求）：**

```
网关(校验/透传 token header) → PermissionInterceptor
   ├─ ① JWT 验签（HS256）→ ② 过期校验 → ③ type==access → ④ 黑名单查询
   ├─ 通过 → request.setAttribute("userId", ...) → 控制器
   └─ 失败 → 401 JSON
之后 @RequirePermission("xxx:list") 按权限点二次鉴权（超管 user_id=1 / *:*:* 旁路）
```

**令牌生命周期管理（Redis 实现"可吊销"）：**

| 场景 | 行为 |
|---|---|
| 登录 | 签发双 token；`auth:refresh:{userId}` 覆盖写入（旧 refresh 立即失效） |
| access 过期（30min） | 前端 401 → 单飞调 `/sys/refresh` → 旋转出新双 token（旧 refresh 失效） |
| 登出 | access 的 jti 写入 `auth:blacklist:{jti}`（TTL=剩余有效期）+ 删除 refresh 记录 |
| 禁用账号 | 需等 access 过期/登出才完全失效（当前无主动踢人接口，见 §5） |
| 7 天无操作 | refresh 过期 → 前端清登录态回登录页 |

**前端配合（mall-web）：**
- `localStorage` 存 `token` + `refreshToken`（`MALL_WEB_REFRESH_TOKEN`）
- 请求拦截器自动带 `token` header；响应拦截器 401 → **单飞刷新**（并发只刷一次）→ 重放原请求；刷新失败清登录态跳 `#/login`

### 2.2 C 端会员（mall-ui/mall-weapp → 业务服务）

```
签发端（3 个，全部替代原来的"明文 memberId 当 token"）：
  微博 OAuth   → mall-auth OAuth2Controller     → redirect ?token=<JWT>
  微信 OAuth   → mall-third-party WxPortalController.callBack → redirect ?token=<JWT>
  微信扫码登录  → WxPortalController.loginStatus（轮询）→ 返回 JWT
  账号密码登录  → mall-auth AuthApiController /auth/login → data.token

统一工具：mall-common MemberJwtUtils（member.jwt 配置，HS256，默认 7 天单 token）
  → 通过 AutoConfiguration.imports 注册（各服务默认包扫描不到 com.mall.common）

消费端（JWT 解析替代 Long.valueOf(token)）：
  mall-order LoginUserInterceptor（/api/** 无/无效 token → 401）
  mall-cart  CartInterceptor（无效 → 临时用户，不阻塞购物车）
  mall-member MemberApiController / MemberReceiveAddressController（JWT 优先，兼容旧参数；
              地址保存/更新 memberId 以 JWT 为准，防伪造归属）
```

### 2.3 双域隔离设计

| | 管理端 | C 端会员 |
|---|---|---|
| 配置 | `auth.jwt.*` | `member.jwt.*` |
| secret | 独立 | 独立 |
| 有效期 | access 30min / refresh 7d | 7d 单 token |
| 可吊销 | ✅ Redis 黑名单 + refresh 旋转 | ❌（升级路径见 §5） |
| 用户域 | sys_user | ums_member |

---

## 2.5 功能使用指南（怎么用）

### ① 登录 / 刷新 / 登出

```bash
# 登录（先 GET /api/captcha.jpg?uuid=xxx 拿验证码图，Redis 存 captcha:{uuid}）
curl -X POST http://<网关>/api/sys/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"******","captcha":"12345","uuid":"xxx"}'
# 返回：{ "code":0, "token":"<access>", "refreshToken":"<refresh>", "expiresIn":1800 }

# access 过期后（HTTP 401）：用 refresh token 换新双 token（旧 refresh 立即作废）
curl -X POST http://<网关>/api/sys/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refresh>"}'

# 登出（access 拉黑 + refresh 吊销）
curl -X POST http://<网关>/api/sys/logout -H 'token: <access>'
```

前端（mall-web）已自动处理：access 过期 → 响应拦截器单飞调 `/sys/refresh` → 重放原请求，用户无感；7 天无操作才需要重新登录。

### ② 踢人 / 禁用即时生效

**界面方式**：权限管理 → 用户管理 → 修改 → 把「状态」改为禁用 → 保存（自动踢）。
**接口方式**（需 `sys:user:update` 权限）：

```bash
curl -X POST http://<网关>/api/sys/user/kick \
  -H 'token: <管理员access>' -H 'Content-Type: application/json' \
  -d '{"userId":"2089415983476944897"}'
```

**效果**：该用户**所有已签发的 access token 立即 401**（`auth:disabled:{userId}` 标记命中即拒），refresh token 同时吊销、无法续期 → 强制重新登录。重新登录成功后标记自动清除。

> 适用场景：账号被盗、员工离职、违规操作临时冻结。注意区别：「禁用账号」会拦截登录，「踢人」只吊销当前登录态（用户还能再登录），按需使用。

### ③ 权限授权（菜单/按钮）

详见 `docs/permission-system.md` §5：菜单管理建菜单/按钮（type 0/1/2，perms 按 `模块:实体:操作`）→ 角色管理勾选授权树 → 用户绑定角色 → 重新登录生效。按钮级用前端 `v-perms` 指令 + 后端 `@RequirePermission` 注解双重控制。

### ④ 密钥与过期时间配置

```yaml
# nacos mall-auth.yaml
auth:
  jwt:
    secret: <生产环境强随机值，≥32 字节>   # 默认占位值仅供开发！
    access-expire-seconds: 1800          # access 有效期（秒）
    refresh-expire-seconds: 604800       # refresh 有效期（秒）
```

修改有效期后，已签发 token 按各自原到期时间自然失效，无需额外处理。**secret 修改会让所有已签发 token 失效**（签名不匹配），相当于全员强制重新登录。

---

## 3. 三代架构对比

| 维度 | ① renren/mall-admin | ② Session（20260818） | ③ JWT 混合（20260819，当前） |
|---|---|---|---|
| 认证载体 | UUID token 存表 | JSESSIONID + Redis 会话 | access JWT（自包含身份） |
| 服务端状态 | 有（sys_user_token） | 有（Redis session） | **无**（仅 refresh 凭证 + 黑名单） |
| 主动失效 | 删 token 行 | session.invalidate | 拉黑 jti + 删 refresh |
| 踢人/禁用即时生效 | ✅ | ✅ | ⚠️ 需登出或等 access 过期（可加踢人接口） |
| 水平扩展 | 需共享 DB | Redis 共享会话（可，有序列化开销） | ✅ 天然无状态，任意扩缩容 |
| 网关统一鉴权 | 难 | 难（要查共享存储） | ✅ 本地验签 |
| 发布重启影响 | token 表在 DB，无影响 | ⚠️ 全部掉线（会话丢失） | ✅ 无影响 |
| 多端（Web/小程序/App） | Cookie 不友好 | Cookie 不友好 | ✅ header 通用 |
| 跨服务身份传递 | 传 token 查表 | 传 sessionId 回源 | ✅ 自包含，本地解析 |
| 前端复杂度 | 低 | 低 | ⚠️ 需处理 401 刷新/重放 |
| 安全风险 | token 表泄露面大 | session 固定/CSRF 面 | ⚠️ token 泄露难撤回（黑名单缓解）、密钥管理 |
| 性能 | 每请求查表 | 每请求查 Redis | ✅ 本地验签（黑名单 1 次 Redis GET） |
| 密码存储 | SHA-256 无盐 | BCrypt | BCrypt |
| 验证码 | 无/表 | Redis（用后即删） | Redis（用后即删） |

---

## 4. 优缺点

### 4.1 优点（第三代 JWT 混合）

1. **无状态 + 水平扩展**：access token 本地验签，多实例/弹性伸缩无会话问题，网关层可统一鉴权
2. **主动失效能力**（对比纯 JWT 的关键改进）：refresh token 存 Redis 可吊销、刷新即旋转；登出通过黑名单立即使 access 失效——保留了 Session 方案"能踢人"的核心能力
3. **短有效期控制泄露面**：access 30 分钟，泄露后影响窗口小；refresh 7 天且可吊销
4. **多端统一**：Web/小程序/App 同一套 header token 机制
5. **C 端安全提升**：微博/微信/账号密码登录从"明文 memberId 塞 URL"改为签名 JWT，杜绝伪造身份；地址归属改为 JWT 为准，堵住越权写
6. **双域隔离**：管理端/C 端独立 secret、独立生命周期，互不影响
7. **权限实时性保留**：权限点仍每次查库（`queryAllPerms`），授权变更即时生效，不被 token 内的快照数据拖累

### 4.2 缺点 / 权衡

1. **黑名单/禁用标记是有状态的**：踢人、登出依赖 Redis 记录，Redis 不可用时这些能力失效（但登录/验证码本来也依赖 Redis，影响面一致）
3. **前端复杂度上升**：需要 401 单飞刷新、请求重放、双 token 存储管理，比 Session 的"自动带 Cookie"多一层逻辑
4. **密钥管理是安全命门**：`auth.jwt.secret` / `member.jwt.secret` 泄露可伪造任意身份；当前仓库里是默认占位值，生产必须通过 Nacos 覆盖为强随机值
5. **C 端仍是单 token**：7 天无黑名单，登出后 token 仍可用到过期（升级路径：照抄 `JwtTokenService` 思路，Redis key 前缀换 `member:`）
6. **无 refresh token 复用检测**：被偷的 refresh token 与合法 token 同时使用时，旋转机制不会告警（高级方案是记录 token 家族做异常检测）
7. **硬编码第三方密钥**：微博/微信 client_secret 仍在代码里（独立于 JWT 的问题，建议挪 Nacos）

---

## 5. 剩余演进建议

1. ✅ **踢人/禁用即时生效**（已实现，提交 `6712368`）：`POST /sys/user/kick` + 禁用账号自动踢，`auth:disabled:{userId}` 标记命中即拒，重新登录自动清除
2. **C 端双 token + 黑名单**：`MemberJwtUtils` 扩展为与 `JwtTokenService` 同构（member 前缀 + refresh 旋转），mall-ui 补 401 刷新逻辑
3. **密钥外置**：两个 secret 都从仓库默认值改为 Nacos 强随机值；第三方 client_secret 同步外置
4. **refresh token 家族检测**（可选）：Redis 记录 token 家族，检测异常重用
5. **网关下沉校验**（可选）：在 Spring Cloud Gateway 做 JWT 验签，业务服务只信网关透传的身份头

---

## 附录：关键代码位置（release_20260819）

| 组件 | 位置 |
|---|---|
| 管理端 JWT 服务 | `mall-auth/.../perm/jwt/JwtTokenService.java`、`JwtProperties.java` |
| 登录/刷新/登出 | `mall-auth/.../perm/controller/SysLoginController.java` |
| 鉴权拦截器 | `mall-auth/.../perm/interceptor/PermissionInterceptor.java` |
| 会员 JWT 工具 | `mall-common/.../common/jwt/MemberJwtUtils.java`、`MemberJwtAutoConfiguration.java` |
| C 端签发（微博） | `mall-auth/.../controller/OAuth2Controller.java` |
| C 端签发（微信） | `mall-third-party/.../controller/WxPortalController.java` |
| C 端签发（账号密码） | `mall-auth/.../app/controller/api/AuthApiController.java` |
| C 端消费 | `mall-order/.../interceptor/LoginUserInterceptor.java`、`mall-cart/.../interceptor/CartInterceptor.java`、`mall-member/.../MemberApiController.java` |
| 前端 401 刷新 | `mall-web/src/utils/request.ts`、`mall-web/src/store/modules/user.ts` |
| 网关路由 | `nacos-config/mall-gateway.yaml`（/api/sys/** → mall-auth，schedule 留 mall-admin） |
