package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * 首页大促横条（mall-ui 首页/多页共用 AppPromo）
 */
@Data
@TableName("sms_home_promo")
public class PromoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 促销主标题1（如：618 星际狂欢节）
     */
    private String title1;
    /**
     * 促销主标题2（如：全场低至 1 折）
     */
    private String title2;
    /**
     * 促销描述
     */
    private String description;
    /**
     * 优惠码（如：NEBULA-618）
     */
    private String code;
    /**
     * 按钮文案
     */
    private String btnText;
    /**
     * 按钮跳转链接
     */
    private String btnLink;
    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}
