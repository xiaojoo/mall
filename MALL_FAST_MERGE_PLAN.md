# mall-fast 模块合并方案

## 概述

mall-fast 是基于 Shiro + JWT 的独立后台管理系统，需将其功能拆分合并到各微服务中，最终移除 mall-fast 模块。

## 一、sys 模块 → mall-member

### 迁移目标包：`com.mall.member.sys`

| 原文件 | 迁移位置 | 说明 |
|--------|---------|------|
| `SysLoginController` | `controller/SysLoginController` | 登录/登出，TODO: 替换 Shiro 为 Spring Security |
| `SysUserController` | `controller/SysUserController` | 用户管理 |
| `SysRoleController` | `controller/SysRoleController` | 角色管理 |
| `SysMenuController` | `controller/SysMenuController` | 菜单管理 |
| `SysConfigController` | `controller/SysConfigController` | 系统配置 |
| `SysLogController` | `controller/SysLogController` | 日志管理 |
| `AbstractController` | `controller/AbstractController` | TODO: 替换 Shiro 为 SecurityContextHolder |
| 所有 Entity | `entity/` | 9个实体类 |
| 所有 DAO | `dao/` | 10个 Mapper 接口 |
| 所有 Service/Impl | `service/` | 11个服务类 |
| Form 类 | `form/` | SysLoginForm, PasswordForm |
| `ShiroService` + Impl | `service/` | TODO: 迁移后替换为 Spring Security |
| `JWTRealm/Filter/Token/Generator` | `jwt/` | TODO: 迁移后替换为 Spring Security JWT Filter |
| `SysConfigRedis` | `redis/` | Redis 缓存配置 |

### Shiro → Spring Security 迁移要点

1. `AbstractController` 中 `SecurityUtils.getSubject().getPrincipal()` → `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
2. `ShiroServiceImpl` → `UserDetailsService` 实现
3. `JWTFilter` → `JwtAuthenticationFilter`（extends `OncePerRequestFilter`）
4. `JWTRealm` → `JwtAuthenticationProvider`
5. `@RequiresPermissions` → `@PreAuthorize`
6. `Sha256Hash` → `BCryptPasswordEncoder`

## 二、oss 模块 → mall-third-party

### 迁移目标包：`com.mall.thirdparty.oss`

| 原文件 | 迁移位置 | 说明 |
|--------|---------|------|
| `SysOssController` | `controller/SysOssController` | 文件上传（移除 `@RequiresPermissions`） |
| `SysOssService/Impl` | `service/` | OSS 服务 |
| `SysOssEntity` | `entity/SysOssEntity` | 文件记录实体 |
| `SysOssDao` | `dao/SysOssDao` | Mapper |
| `CloudStorageService` | `cloud/CloudStorageService` | 云存储接口 |
| `CloudStorageConfig` | `cloud/CloudStorageConfig` | 配置类 |
| `OSSFactory` | `cloud/OSSFactory` | 工厂类 |
| `AliyunCloudStorageService` | `cloud/` | 阿里云 OSS（已有依赖） |
| `QiniuCloudStorageService` | `cloud/` | 七牛云（需加依赖） |
| `QcloudCloudStorageService` | `cloud/` | 腾讯云（需加依赖） |

### 需新增依赖（mall-third-party/pom.xml）

- `com.qiniu:qiniu-java-sdk:${qiniu.version}`
- `com.qcloud:cos_api:${cos.version}`

### 跨服务依赖

`SysOssController` 依赖 `SysConfigService`，需通过 OpenFeign 调用 mall-member 或将配置存储移到 Redis/本地配置。

## 三、job 模块 → 待定

### 建议：迁移到 XXL-Job 或 Spring @Scheduled

| 方案 | 优点 | 缺点 |
|------|------|------|
| XXL-Job | 统一调度平台、可视化管理、动态配置 | 需要部署调度中心 |
| Spring @Scheduled | 简单、无额外组件 | 不支持动态管理、无 UI |
| Quartz（现有） | 成熟、支持持久化 | 与 Spring Boot 3 集成需适配 |

**推荐**：先使用 Spring `@Scheduled`，后续视规模升级到 XXL-Job。

### 迁移步骤
1. 将 `ScheduleJobService` 中的任务提取为独立 `@Scheduled` Bean
2. `TestTask` → 各微服务中的定时任务类
3. `ScheduleJobLogEntity` → 各微服务的日志表或统一日志服务

## 四、app 模块 → mall-auth

### 迁移目标包：`com.mall.auth.app`

| 原文件 | 迁移位置 | 说明 |
|--------|---------|------|
| `AppLoginController` | `controller/AppLoginController` | APP 登录（迁移 Swagger → SpringDoc） |
| `AppRegisterController` | `controller/AppRegisterController` | APP 注册 |
| `AppTestController` | `controller/AppTestController` | 测试用（可删除） |
| `UserService/Impl` | `service/` | App 用户服务 |
| `UserEntity` | `entity/UserEntity` | App 用户实体 |
| `UserDao` | `dao/UserDao` | App 用户 Mapper |
| `JwtUtils` | `utils/JwtUtils` | JWT 工具类（需升级 jjwt 到 0.12.x） |
| `LoginForm/RegisterForm` | `form/` | 表单 |
| `Login/LoginUser` | `annotation/` | 自定义注解 |
| `AuthorizationInterceptor` | `interceptor/` | 拦截器 |
| `LoginUserHandlerMethodArgumentResolver` | `resolver/` | 参数解析器 |
| `WebMvcConfig` | `config/WebMvcConfig` | MVC 配置 |

## 五、common 模块 → mall-common

### 已迁移到 `com.mall.member.sys.common`（临时）

待整理后应迁移到 `mall-common`：

| 类 | 说明 | 是否需要保留 |
|----|------|-------------|
| `R` | 统一返回结果 | ✅ 合并到 mall-common |
| `PageUtils` | 分页工具 | ⚠️ 评估与现有分页是否重复 |
| `Constant` | 常量 | ✅ |
| `ConfigConstant` | 配置常量 | ✅ |
| `Query` | 查询封装 | ⚠️ 评估重复 |
| `RRException` | 自定义异常 | ⚠️ 评估重复 |
| `RRExceptionHandler` | 全局异常处理 | ⚠️ 评估重复 |
| `ValidatorUtils` | 校验工具 | ✅ |
| `Assert` | 断言注解 | ✅ |
| `validator/group/*` | 校验分组 | ✅ |
| `SysLog` + `SysLogAspect` | 系统日志 | ✅ |
| `IPUtils` | IP 工具 | ⚠️ 评估重复 |
| `DateUtils` | 日期工具 | ⚠️ 评估重复 |
| `RedisUtils` | Redis 工具 | ⚠️ 评估重复 |
| `xss/*` | XSS 防护 | ✅ |
| `ShiroUtils` | Shiro 工具 | ❌ 删除（迁移后不需要） |
| `RedisKeys` | Redis Key | ✅ |
| `SpringContextUtils` | Spring 上下文 | ✅ |
| `MapUtils` | Map 工具 | ⚠️ 评估重复 |
| `BeanFindUtils` | Bean 查找 | ⚠️ 评估重复 |

## 六、Shiro → Spring Security 迁移方案

### 整体架构

```
请求 → JwtAuthenticationFilter → SecurityContextHolder → Controller
                                      ↑
                              JwtAuthenticationProvider (验证 Token)
                                      ↑
                              UserDetailsService (加载用户权限)
```

### 具体步骤

1. **添加依赖**：`spring-boot-starter-security` + `jjwt-api/jjwt-impl/jjwt-jackson`
2. **实现 UserDetailsService**：替代 `ShiroServiceImpl`
3. **JwtAuthenticationFilter**：替代 `JWTFilter`，解析 Token 设置 SecurityContext
4. **SecurityConfig**：配置 URL 权限、过滤器链
5. **替换注解**：`@RequiresPermissions("sys:user:list")` → `@PreAuthorize("hasAuthority('sys:user:list')")`
6. **密码加密**：`Sha256Hash` → `BCryptPasswordEncoder`

## 七、datasource 模块

动态数据源配置（`DynamicDataSource` 等），暂不迁移。如 mall-member 需要多数据源支持，后续单独引入 `dynamic-datasource-spring-boot-starter`。
