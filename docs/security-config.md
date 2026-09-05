# JWT 密钥配置（单源覆写，防漂移）

> 仓库里 `member.jwt.secret` / `auth.jwt.secret` 的**唯一默认源**在代码里：
> - `mall-common/.../MemberJwtUtils`（`@ConfigurationProperties(prefix = "member.jwt")`）
> - `mall-auth/.../perm/jwt/JwtProperties`（`@ConfigurationProperties(prefix = "auth.jwt")`）
>
> 各模块**不要**在 nacos-config / application.yml 里各自复制一份 secret，否则多个文件可能漂移不一致，导致签名/校验失败。

## 生产统一覆写方式（二选一，推荐①）

### ① 环境变量（所有服务统一，单点维护）
Spring 松弛绑定会把环境变量映射到配置项，无需改任何配置文件：

| 配置项 | 环境变量 | 适用服务 |
|---|---|---|
| `auth.jwt.secret` | `AUTH_JWT_SECRET` | mall-auth（签发）、mall-admin（校验） |
| `member.jwt.secret` | `MEMBER_JWT_SECRET` | mall-auth/mall-third-party（签发）、mall-order/mall-cart/mall-member（校验） |

例如在 Jenkinsfile / docker-compose 给服务容器统一注入：
```yaml
environment:
  AUTH_JWT_SECRET: "<≥32字节的随机串>"
  MEMBER_JWT_SECRET: "<≥32字节的随机串>"
```

### ② 只改一处 Nacos 配置文件
若坚持用配置中心，**每个 secret 只保留一个**写入点：
- `auth.jwt.secret` → 仅 `nacos-config/mall-auth.yaml`（mall-admin 改用 `AUTH_JWT_SECRET` 环境变量或与 mall-auth 同值）
- `member.jwt.secret` → 仅 `nacos-config/mall-member.yaml`，其余服务通过环境变量读取

## 一致性校验
- `auth.jwt.secret`：mall-auth 与 mall-admin **必须完全相同**，否则 mall-admin 接口全部 401。
- `member.jwt.secret`：签发方与所有消费方业务服务必须相同。

## 遗留清理
若历史 nacos 配置已把 secret 复制到多处，建议删除多余副本，只保留上述单一写入点；改完后 `docker restart` 相关服务。
