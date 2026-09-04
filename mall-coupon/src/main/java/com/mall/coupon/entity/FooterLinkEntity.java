package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * 页脚链接（mall-ui AppFooter 页脚三列，按 group_name 分组展示）
 */
@Data
@TableName("sms_footer_link")
public class FooterLinkEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 列标题（如：购物指南 / 配送服务 / 关于我们）
     */
    private String groupName;
    /**
     * 列排序（同组内数字小的列靠前）
     */
    private Integer groupSort;
    /**
     * 链接名称（如：购物流程）
     */
    private String name;
    /**
     * 跳转链接（/list 等站内路径或 https:// 外部链接）
     */
    private String url;
    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;
    /**
     * 组内排序
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
