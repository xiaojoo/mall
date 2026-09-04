package com.mall.weapp.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 微信小程序首页聚合数据实体
 * <p>用于首页接口返回的聚合数据，包含Banner、推荐商品、分类入口等</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Data
public class WeappHomeEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Banner广告图列表
     */
    private List<Banner> banners;

    /**
     * 推荐商品列表
     */
    private List<RecommendProduct> recommendProducts;

    /**
     * 分类入口列表
     */
    private List<CategoryEntry> categoryEntries;

    /**
     * Banner广告图
     */
    @Data
    public static class Banner implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 图片地址 */
        private String imageUrl;

        /** 跳转链接 */
        private String linkUrl;

        /** 排序 */
        private Integer sort;

        /** 标题 */
        private String title;
    }

    /**
     * 推荐商品简要信息
     */
    @Data
    public static class RecommendProduct implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 商品SPU ID */
        private Long spuId;

        /** 商品名称 */
        private String spuName;

        /** 商品图片 */
        private String imageUrl;

        /** 价格 */
        private String price;
    }

    /**
     * 分类入口
     */
    @Data
    public static class CategoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 分类ID */
        private Long categoryId;

        /** 分类名称 */
        private String categoryName;

        /** 分类图标 */
        private String iconUrl;
    }
}
