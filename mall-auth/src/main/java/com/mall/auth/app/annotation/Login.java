package com.mall.auth.app.annotation;

import java.lang.annotation.*;

/**
 * app登录效验
 *
 * @author mall
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {
}
