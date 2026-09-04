package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * 首页轮播内容（mall-ui 首页 HERO 轮播）
 *
 * content 为 JSON 字符串，结构（对应前端轮播每屏渲染所需信息）：
 * {
 *   "kicker": "顶部小字",
 *   "title": "主标题第一段",
 *   "highlight1": "主标题高亮词1(glow-c)",
 *   "title2": "主标题第二段(换行后)",
 *   "highlight2": "主标题高亮词2(glow-m)",
 *   "sub": "副标题/描述(可含<b>标签)",
 *   "buttons": [{ "text": "按钮文字", "type": "primary|ghost", "link": "/list" }],
 *   "stats": [{ "num": "2.4", "unit": "亿+", "label": "注册星际用户" }],
 *   "chips": [{ "pos": "a|b|c|d", "text": "全息面板小字" }],
 *   "price": { "label": "限时首发", "value": "9,999", "decimals": ".00" }
 * }
 */
@Data
@TableName("sms_home_carousel")
public class HomeCarouselEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 轮播标识（如：AI智选）
     */
    private String name;
    /**
     * 主题（s1/s2/s3，对应前端主题色）
     */
    private String theme;
    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 轮播内容 JSON
     */
    private String content;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}
