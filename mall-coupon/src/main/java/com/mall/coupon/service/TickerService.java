package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.TickerEntity;

import java.util.List;
import java.util.Map;

/**
 * 首页跑马灯公告
 */
public interface TickerService extends IService<TickerEntity> {

    /**
     * 管理端分页查询（含停用）
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商城端：启用中的公告文本，按 sort 升序
     */
    List<String> listEnabledTexts();
}
