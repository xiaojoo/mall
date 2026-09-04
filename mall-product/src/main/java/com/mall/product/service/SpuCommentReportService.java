package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.entity.SpuCommentReportEntity;

import java.util.Map;

/**
 * 商品评论举报
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
public interface SpuCommentReportService extends IService<SpuCommentReportEntity> {

    PageUtils queryPage(Map<String, Object> params);
}
