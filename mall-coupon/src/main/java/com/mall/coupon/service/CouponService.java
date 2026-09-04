package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.exception.RRException;
import com.mall.common.to.CouponUseCheckTo;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.CouponEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 优惠券信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
public interface CouponService extends IService<CouponEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * C端-可领取优惠券列表（已发布且在领取时间窗口内；带会员 id 时标记已领取）
     */
    List<CouponEntity> getMemberCouponList(Long memberId);

    /**
     * 店铺优惠券列表（已发布 + 领取时间窗口内 + 属于该店铺；带会员 id 时标记已领取）
     */
    List<CouponEntity> getShopCouponList(Long brandId, Long memberId);
    /**
     * 管理端-可使用优惠券列表：已发布且在有效窗口内；
     * 传 skuId 时按适用范围过滤（全场通用 useType=0 / 指定分类 useType=1（含祖先分类）/ 指定商品 useType=2），
     * 不传返回全部可用券（商品管理 SKU 展开区展示用）
     */
    List<CouponEntity> listUsable(Long skuId);

    /**
     * C端-当前会员可用优惠券（结算页）：已领取未使用 + 已发布且在有效窗口内 + 门槛满足 + 适用范围匹配订单商品
     *
     * @param memberId 会员 id（null 返回空列表）
     * @param amount   订单商品总额（门槛 min_point 判断）
     * @param skuIds   订单商品 skuId 列表（适用范围过滤，任一命中即可）
     */
    List<CouponEntity> listMemberUsable(Long memberId, BigDecimal amount, List<Long> skuIds);

    /**
     * C端-优惠券使用校验（订单提交时调用）：通过返回优惠金额，否则抛 {@link RRException}
     */
    BigDecimal useCheckCoupon(CouponUseCheckTo to);

    /**
     * 下单成功核销优惠券：将该会员该券的未使用记录标记为已使用（use_type=1），回写订单号
     */
    void consumeCoupon(Long memberId, Long couponId, String orderSn);

    /**
     * 订单取消/关闭回退优惠券：将该会员该券的已使用记录（匹配订单号）回退为未使用（use_type=0）
     */
    void refundCoupon(Long memberId, Long couponId, String orderSn);

    /**
     * C端-领取优惠券（校验发布状态/领取时间/库存/限领，写入领取记录）
     */
    void receiveCoupon(Long memberId, Long couponId);

    /**
     * 发布/下架
     */
    void updatePublish(Long id, Integer publish);

    /**
     * 复制优惠券（生成新券，领取/使用数量归零，默认未发布）
     *
     * @return 新券 id
     */
    Long copyCoupon(Long id);
}

