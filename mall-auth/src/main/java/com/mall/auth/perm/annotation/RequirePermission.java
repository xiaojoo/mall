package com.mall.auth.perm.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 标注在Controller方法上，表示需要指定权限才能访问
 *
 * 使用示例：
 *   @RequirePermission("sys:user:list")
 *   public Result<?> list() { ... }
 *
 *   @RequirePermission(value = {"sys:user:save", "sys:user:update"}, logical = Logical.OR)
 *   public Result<?> saveOrUpdate() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限标识，支持多个
     */
    String[] value();

    /**
     * 多个权限标识的逻辑关系：AND-全部满足 OR-满足其一
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}
