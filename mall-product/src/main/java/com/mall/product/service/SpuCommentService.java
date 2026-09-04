package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.entity.SpuCommentEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 商品评价
 *
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 发表评价（含业务规则）：
     * - 评论归属：JWT 解析会员 id，缺失时保留前端传入昵称兜底
     * - 非回复评论：每个用户同一商品最多评价 2 条，超限抛 {@link com.mall.common.exception.RRException}
     * - 第 2 条自动标记为追加评论（commentType=2），不参与评分/好评率统计
     *
     * @param spuComment 评论实体
     * @param request    当前请求（用于解析会员 JWT）
     */
    void saveComment(SpuCommentEntity spuComment, HttpServletRequest request);
}

