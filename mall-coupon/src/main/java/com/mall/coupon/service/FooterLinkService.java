package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.FooterLinkEntity;

import java.util.List;
import java.util.Map;

/**
 * 页脚链接
 */
public interface FooterLinkService extends IService<FooterLinkEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商城端：启用中的页脚链接（按列排序 + 组内排序）
     */
    List<FooterLinkEntity> listEnabled();
}
