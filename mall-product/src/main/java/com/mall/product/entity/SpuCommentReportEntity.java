package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品评论举报
 * 
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@Data
@TableName("pms_spu_comment_report")
public class SpuCommentReportEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * 被举报评论id
	 */
	private Long commentId;
	/**
	 * 商品id
	 */
	private Long spuId;
	/**
	 * 商品名字
	 */
	private String spuName;
	/**
	 * 举报人昵称
	 */
	private String memberNickName;
	/**
	 * 举报原因
	 */
	private String reason;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 处理状态[0-待处理，1-已处理]
	 */
	private Integer status;

}
