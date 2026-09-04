package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.HomeNavEntity;

import java.util.List;
import java.util.Map;

/**
 * 首页快捷导航
 */
public interface HomeNavService extends IService<HomeNavEntity> {

    /**
     * 管理端分页查询（name 模糊 + 状态过滤）
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商城端：启用中的导航列表（按 sort 升序）
     */
    List<HomeNavEntity> listEnabled();
}
