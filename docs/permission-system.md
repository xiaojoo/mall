# 权限系统设计文档（mall-auth / mall-web）

> 适用范围：后台管理系统（mall-web 前端 + mall-auth 权限服务）
> 数据库：`mall_auth`（独立库）
> 最后更新：2026-08-18

---

## 1. 概述

本项目权限系统采用经典的 **RBAC（Role-Based Access Control，基于角色的访问控制）** 模型，前后端分离实现：

- **后端**：`mall-auth` 微服务，独立数据库 `mall_auth`，负责登录认证、权限点管理、接口级鉴权
- **前端**：`mall-web`（Vue 3 + Pinia + Vue Router + Element Plus），负责菜单渲染、页面级与按钮级权限控制
- **权限模型**：`用户 → 角色 → 菜单/权限点` 三级关联，权限粒度细分为 **目录 / 菜单 / 按钮** 三级

权限点采用统一的字符串规范（如 `product:category:list`），前后端共用同一套标识，菜单与按钮权限在**同一张表**（`sys_menu`）中管理。

---

## 2. 整体架构

```
┌──────────────┐      ┌──────────────┐      ┌──────────────────────┐
│   mall-web    │ ───▶ │  mall-gateway │ ───▶ │      mall-auth        │
│  (Vue 3 SPA)  │      │  (网关/鉴权)  │      │  Spring Boot 服务      │
└──────────────┘      └──────────────┘      └──────────┬───────────┘
       │                                                │
       │ ① 登录（用户名/密码/验证码）                      │ Redis（验证码存储）
       │ ② 携带 token 调用业务接口                        ▼
       ▼                                         ┌──────────────┐
┌──────────────┐                                │  mall_auth 库  │
│  业务微服务    │  @RequirePermission 接口鉴权    │  6 张权限表    │
│  mall-*      │ ◀───────────────────────────── │              │
└──────────────┘                                └──────────────┘
```

**职责划分：**

| 层 | 职责 |
|---|---|
| mall-web 前端 | 登录页、路由守卫（页面级）、`v-perms` 指令（按钮级）、侧边栏菜单按权限过滤 |
| mall-gateway | 统一入口，透传/校验登录态 |
| mall-auth | 登录认证（BCrypt + 图形验证码）、用户/角色/菜单 CRUD、权限点查询、`@RequirePermission` 接口鉴权 |
| mall_auth 库 | 权限数据唯一来源 |

---

## 3. 数据模型

### 3.1 表结构

| 表 | 说明 | 关键字段 |
|---|---|---|
| `sys_user` | 系统用户 | `user_id`(雪花ID)、`username`(唯一)、`password`(BCrypt)、`salt`、`status`(0禁用/1正常)、`dept_id` |
| `sys_role` | 角色 | `role_id`、`role_name`、`role_code`(唯一)、`status` |
| `sys_menu` | 菜单/权限点（目录+菜单+按钮） | `menu_id`、`parent_id`(顶级为0)、`name`、`url`、`perms`(权限标识)、`type`(0目录/1菜单/2按钮)、`icon`、`order_num` |
| `sys_user_role` | 用户-角色关联 | `user_id` + `role_id`，唯一键 `uk_user_role` |
| `sys_role_menu` | 角色-菜单关联 | `role_id` + `menu_id`，唯一键 `uk_role_menu` |
| `sys_log` | 操作日志 | 记录用户操作（配合 `SysLogAspect`） |

### 3.2 权限模型关系

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
   │                                                          │
   │                                                          ├─ type=0 目录（不挂权限点）
   │                                                          ├─ type=1 菜单（可挂 perms）
   │                                                          └─ type=2 按钮（perms 必填）
   └── user_id = 1 视为超级管理员，拥有通配权限 *:*:*
```

### 3.3 权限点规范

权限标识统一为 `模块:实体:操作` 三段式，前后端**完全一致**：

| 操作 | 权限串示例 | 说明 |
|---|---|---|
| 查看列表/详情 | `product:category:list` | 通常挂在 type=1 菜单上，兼作菜单显隐条件 |
| 新增 | `product:category:save` | type=2 按钮 |
| 修改 | `product:category:update` | type=2 按钮 |
| 删除 | `product:category:delete` | type=2 按钮 |
| 超管通配 | `*:*:*` | 仅 user_id=1 的用户，旁路所有权限校验 |

---

## 4. 核心流程

### 4.1 登录认证

1. 前端访问 `/api/captcha.jpg?uuid=xxx` 获取图形验证码，后端将验证码存入 Redis（key：`captcha:{uuid}`）
2. 提交 `POST /sys/login`（username / password / captcha / uuid）
3. 后端校验：验证码（用后即删）→ 用户存在 → `status=1` → BCrypt 密码匹配
4. 生成 UUID token，写入 `HttpSession`（`token`、`userId`），返回 `{ code:0, token }`
5. 前端保存 token，后续请求通过 header `token` 携带（网关校验后透传用户身份）

> 注意：当前实现基于 **HttpSession + UUID token**，登录态存于服务端会话。代码注释明确标注"生产环境建议用 JWT"。

### 4.2 权限拉取（前端菜单/按钮的数据来源）

```
GET /sys/user/perms  （前端登录后调用）
        │
        ▼
sysUserService.queryPermsList(userId)
        │   userId == 1 → 直接返回 ["*:*:*"]
        ▼
sysMenuService.queryAllPerms(userId)
        │
        ▼
SELECT DISTINCT m.perms FROM sys_menu m
  JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
  JOIN sys_user_role ur ON rm.role_id = ur.role_id
 WHERE ur.user_id = #{userId}
   AND m.perms IS NOT NULL AND m.perms != ''
```

- 返回当前用户**全部权限标识**（含按钮 type=2，SQL 去重、去空）
- 菜单（type=1）的 `perms` 同时充当**菜单显隐条件**与**接口权限点**

### 4.3 前端菜单过滤（页面级）

前端采用**静态路由 + 按权限过滤**方案（`src/store/modules/user.ts`）：

1. 路由表 `src/router/routes.ts` 定义全部页面，页面通过 `meta.perms` 标记所需权限
2. 登录后 `userInfo()` 拉取 perms，调用 `filterMenuByPerms(constantRoute, perms)`：
   - 路由无 `meta.perms` → 直接显示（如目录节点）
   - 有 `meta.perms` → 用户权限列表包含任一即显示
   - **子级全部被过滤掉的父级自动隐藏**
   - 超管（`*:*:*`）不过滤，看到全部
3. 侧边栏组件 `src/layout/menu/index.vue` 按过滤结果渲染
4. 路由守卫 `src/permisstion.ts`：访问带 `meta.perms` 的页面时二次校验，无权限跳回首页

### 4.4 按钮级权限（v-perms 指令）

自定义指令 `src/directives/perms.ts`，支持三种写法：

```html
<!-- 单个权限 -->
<el-button v-perms="'sys:user:save'">新增</el-button>

<!-- 多个权限，满足任一 -->
<el-button v-perms="['sys:user:save', 'sys:user:update']">保存</el-button>

<!-- 多个权限，全部满足（mode: 'and'） -->
<el-button v-perms="{ value: ['sys:user:save', 'sys:user:update'], mode: 'and' }">保存</el-button>
```

无权限时指令直接移除该元素（`vnode.parentNode.removeChild`）。

### 4.5 后端接口鉴权（服务端兜底）

业务接口通过 `@RequirePermission` 注解声明所需权限：

```java
@GetMapping("/list")
@RequirePermission("sys:menu:list")
public List<SysMenuEntity> list() { ... }
```

`PermissionInterceptor` 拦截逻辑：

1. 从 session / request attribute 取 `userId`，取不到返回 401
2. `userId == 1`（超管）直接放行
3. 查询用户权限列表，`@RequirePermission` 支持 `logical = AND / OR`（默认 AND）
4. 无权限返回 403

> **重要**：前端隐藏按钮只是体验优化，**真正的安全边界在后端注解**。新增敏感接口务必加 `@RequirePermission`。

---

## 5. 使用指南

### 5.1 初始化（首次部署）

在 `mall_auth` 库按顺序执行 `db/` 下脚本（全部**可重复执行**，先 DELETE 清理再插入，不会影响其他角色授权）：

```bash
# 1. 建表（IF NOT EXISTS）
db/sys_permission.sql

# 2. 业务菜单 + 按钮 + 超管绑定
db/sys_permission_menus.sql

# 3. 按钮权限全量种子（业务删除/修改按钮 + 权限管理组菜单/按钮）
db/sys_permission_buttons.sql
```

### 5.2 日常使用流程

| 步骤 | 入口 | 操作 |
|---|---|---|
| 1. 维护菜单/按钮 | 权限管理 → 菜单管理 | 新增目录(type=0)/菜单(type=1)/按钮(type=2)，perms 按 `模块:实体:操作` 规范填写 |
| 2. 创建角色 | 权限管理 → 角色管理 → 新增 | 填写角色名称，授权树勾选菜单 + 按钮 |
| 3. 分配用户 | 权限管理 → 用户管理 → 新增/修改 | 创建用户并勾选角色 |
| 4. 生效 | — | 用户重新登录（每次登录/刷新都会重新拉取 perms） |

### 5.3 前端接入新功能

```ts
// ① routes.ts 新增路由，用 meta.perms 标记权限
{
  path: '/coupon',
  component: () => import('@/layout/index.vue'),
  meta: { title: '优惠券管理', hidden: true },
  children: [{
    path: '/coupon/list',
    component: () => import('@/views/coupon/list.vue'),
    meta: { title: '优惠券列表', perms: 'coupon:coupon:list' },
  }],
}
```

```html
<!-- ② 页面内按钮用 v-perms 控制 -->
<el-button v-perms="'coupon:coupon:delete'">删除</el-button>
```

```sql
-- ③ 菜单管理界面新增对应菜单/按钮（或直接 SQL 种子）
```

### 5.4 后端接入新接口

```java
@PostMapping("/save")
@RequirePermission("coupon:coupon:save")
public Map<String, Object> save(@RequestBody CouponEntity coupon) { ... }
```

---

## 6. 种子脚本说明

| 脚本 | 内容 | menu_id 段 |
|---|---|---|
| `sys_permission.sql` | 表结构（CREATE TABLE IF NOT EXISTS） | — |
| `sys_permission_menus.sql` | 业务目录/菜单（商品/库存/会员/营销/调度）+ 删除按钮 + 超管绑定 | 4~7、201~210、401~409、501~502、601~612、701、901~926 |
| `sys_permission_buttons.sql` | 权限管理组（801~805）+ acl 按钮（927~935）+ 业务修改按钮（936~959）+ 超管绑定 | 801~805、901~959 |

脚本幂等策略：开头 `DELETE`（仅清理脚本管辖的 menu_id 与 `role_id=1` 的绑定）→ `INSERT IGNORE`，可反复执行。

### 6.1 旧 mall-admin 权限表清理（已废弃）

权限系统由 mall-auth 重建后，旧后台 mall-admin（renren 版，`mall_admin` 库）中重复/废弃的表可清理，脚本见 `db/drop_mall_admin_duplicate_tables.sql`：

| 分组 | 表 | 说明 |
|---|---|---|
| A：与 mall_auth 重复 | `sys_user` `sys_role` `sys_menu` `sys_user_role` `sys_role_menu` `sys_log` | 权限数据已由 mall_auth 接管，删除 |
| B：无引用废弃 | `sys_user_token` `sys_captcha` `sys_config` `sys_oss` `tb_user` `QRTZ_*`(×11) | 旧机制残留/无调用方，删除 |
| C：⚠️ 仍在使用 | `schedule_job` `schedule_job_log` | mall-web 任务调度页仍依赖 mall-admin 提供接口，**暂不删**，待定时任务迁移后处理 |

> 注意：网关路由 `/api/sys/**` 目前仍指向 mall-admin（`nacos-config/mall-gateway.yaml`），若 mall-auth 已接管后台接口，需同步把该路由改为指向 mall-auth（保留 `/api/sys/schedule**` 给 mall-admin 或迁移定时任务）。

---

## 7. 优缺点分析

### 7.1 优点

1. **模型简单清晰**：经典 RBAC 三级关联，菜单/按钮共表（`sys_menu`），学习成本低
2. **权限粒度细**：目录 → 菜单 → 按钮三级，可精确控制到单个按钮（新增/修改/删除独立授权）
3. **前后端权限串统一**：`模块:实体:操作` 一段字符串贯穿前端 `v-perms`、路由守卫、后端注解，无映射层
4. **超管旁路机制**：`user_id=1 → *:*:*`，避免超管被误锁，且不影响普通角色
5. **种子脚本可重复执行**：先删后插，数据可随时恢复种子状态，便于多环境部署
6. **独立库独立服务**：`mall_auth` 与业务库隔离，权限数据集中管理
7. **接口鉴权有服务端兜底**：前端隐藏只是体验，`@RequirePermission` 拦截器保证后端安全边界

### 7.2 缺点

1. **会话方案偏旧**：基于 HttpSession + UUID token（服务端状态），分布式部署/跨域需额外共享会话或改造 JWT（代码注释也承认"生产环境建议用 JWT"）
2. **静态路由 + 前端过滤**：菜单不是后端动态下发（`/sys/menu/nav` 接口存在但前端未使用），新增/调整菜单需改 `routes.ts` 并**重新构建前端**；权限数据变更需要重新登录/刷新才生效
3. **无数据权限**：仅功能权限（能不能点），**没有数据范围权限**（如"只看本部门数据"），`dept_id` 字段存在但未参与权限计算
4. **perms 字段的逗号分隔是隐性陷阱**：表注释支持 `user:list,user:create` 逗号分隔，但前端 `perms.includes()` 与后端 `queryAllPerms` 都**不做拆分**，写成逗号分隔会静默失效
5. **雪花 ID 精度问题**：`user_id/role_id/menu_id` 为雪花 ID（19 位），超过 JS `Number.MAX_SAFE_INTEGER`，前端必须用字符串处理（项目已通过"后端 Long 转字符串数组"修复，但新开发人员容易踩坑）
6. **无权限缓存**：每次请求都查库（`queryPermsList`），权限量大或高频调用时有性能开销；但好处是权限变更即时生效（无需清缓存）
7. **内存建树**：`getUserMenuList` 用内存 Map 迭代组装菜单树，菜单量极大时存在性能与序列化风险（曾出现树自引用导致的 StackOverflowError，已修复）
8. **验证码强依赖 Redis**：登录验证码存 Redis，Redis 不可用时登录直接不可用

---

## 8. 踩坑记录（历史问题与修复）

| 问题 | 现象 | 根因 | 修复 |
|---|---|---|---|
| 授权了菜单但登录后不显示 | 侧边栏空白 | `queryPermsList` 只遍历菜单树顶层（目录），子菜单上的 perms 收集不到，`/sys/user/perms` 恒返回空数组 | 改为 `queryAllPerms` SQL 直接查全量 perms（含按钮） |
| 角色授权树看不到按钮 | 无法勾选按钮权限 | `/sys/menu/list` 用 `queryNotButtonList`（type != 2）过滤了按钮 | 改为返回全部菜单（含 type=2） |
| 菜单管理弹窗报 `setCurrentKey undefined` | 打开"修改"即报错 | 模板 ref 为 `treeRef`，脚本引用 `menuListTree`，ref 名错配；且树在 popover 内首开未挂载 | 统一 ref 名 + 空值防御 |
| 菜单树序列化死循环 | StackOverflowError | 树节点 `parentId` 自引用 | 建树时跳过自引用节点 |
| 授权回显权限膨胀 | 勾选父级误选未授权子菜单 | el-tree 级联勾选特性 | 回显只勾叶子节点，父级交给级联半选 |
| 雪花 ID 授权回显失败 | 回显/保存错乱 | Long 精度丢失 | `roleIdList/menuIdList` 改字符串数组 |

---

## 9. 演进建议

1. **认证升级 JWT**：替换 HttpSession，支持无状态、跨域、分布式部署；配合网关统一校验
2. **菜单动态下发**：前端改用 `/sys/menu/nav` 动态生成路由，菜单/按钮变更无需重新构建前端
3. **数据权限**：引入部门/数据范围（全部/本部门/本人）维度，配合 `dept_id` 实现行级权限
4. **perms 规范化**：要么强制单权限串（校验禁止逗号），要么前后端统一做拆分
5. **权限缓存 + 变更通知**：`queryPermsList` 加 Redis 缓存，权限变更时主动失效，兼顾性能与实时性
6. **操作日志完善**：`sys_log` + `SysLogAspect` 已具备基础，可补充登录日志、权限变更审计

---

## 附录：关键代码位置

| 组件 | 位置 |
|---|---|
| 登录控制器 | `mall-auth/.../perm/controller/SysLoginController.java` |
| 权限拦截器 | `mall-auth/.../perm/interceptor/PermissionInterceptor.java` |
| 权限注解 | `mall-auth/.../perm/annotation/RequirePermission.java` |
| 用户权限查询 | `mall-auth/.../perm/service/impl/SysUserServiceImpl.java#queryPermsList` |
| 菜单服务 | `mall-auth/.../perm/service/impl/SysMenuServiceImpl.java` |
| 权限 SQL | `mall-auth/src/main/resources/mapper/perm/SysMenuDao.xml` |
| 前端菜单过滤 | `mall-web/src/store/modules/user.ts#filterMenuByPerms` |
| 路由守卫 | `mall-web/src/permisstion.ts` |
| 按钮指令 | `mall-web/src/directives/perms.ts` |
| 路由表 | `mall-web/src/router/routes.ts` |
| 种子脚本 | `db/sys_permission.sql`、`db/sys_permission_menus.sql`、`db/sys_permission_buttons.sql` |
