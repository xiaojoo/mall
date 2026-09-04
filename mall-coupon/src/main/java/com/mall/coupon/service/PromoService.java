package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.PromoEntity;

import java.util.List;
import java.util.Map;

/**
 * 首页大促横条
 */
public interface PromoService extends IService<PromoEntity> {

    /**
     * 管理端分页查询（含停用）
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商城端：启用中的大促，按 sort 升序
     */
    List<PromoEntity> listEnabled();
}
