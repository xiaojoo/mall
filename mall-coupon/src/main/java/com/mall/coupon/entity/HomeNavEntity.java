package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;

/**
 * 首页快捷导航（cat-row：全息设备 … 新品首发）
 * <p>link 为空时前端按名称跳列表页筛选；非空时优先跳配置链接（站内路径或外链）。</p>
 */
@Data
@TableName("sms_home_nav")
public class HomeNavEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 名称（如：全息设备 / 新品首发）
     */
    private String name;

    /**
     * 图标（字符图标，如：⌬）
     */
    private String icon;

    /**
     * 跳转链接（空=按名称跳列表筛选；站内路径 /xxx 或 https:// 外链）
     */
    private String link;

    /**
     * 是否 HOT 标[0-否；1-是]（兼容旧数据）
     */
    private Integer hot;

    /**
     * 标签文字（如 HOT/新品/爆款；空=不显示，兼容旧数据回退 hot）
     */
    private String tag;

    /**
     * 标签颜色（CSS 颜色值，如 #ff2e63；空=默认绿色）
     */
    private String tagColor;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 显示状态[0-停用；1-启用]
     */
    private Integer showStatus;

}
