package com.mall.admin.utils;

public final class AdminConstants {
    private AdminConstants() {}

    /** 超级管理员ID */
    public static final long SUPER_ADMIN = 1L;

    /** 菜单类型 */
    public static final class MenuType {
        public static final int CATALOG = 0;  // 目录
        public static final int MENU = 1;     // 菜单
        public static final int BUTTON = 2;   // 按钮
    }
}
