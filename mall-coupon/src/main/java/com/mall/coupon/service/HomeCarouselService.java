package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.HomeCarouselEntity;

import java.util.List;
import java.util.Map;

/**
 * 首页轮播内容
 */
public interface HomeCarouselService extends IService<HomeCarouselEntity> {

    /**
     * 管理端分页查询（含停用）
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商城端：启用中的轮播，按 sort 升序
     */
    List<HomeCarouselEntity> listEnabled();
}
