package com.mall.coupon.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.coupon.entity.CouponEntity;
import com.mall.coupon.service.CouponService;
import com.mall.common.exception.RRException;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.PageUtils;
import com.mall.common.to.CouponUseCheckTo;
import com.mall.common.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;


/**
 * 优惠券信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RefreshScope
@RestController
@RequestMapping("coupon/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final MemberJwtUtils memberJwtUtils;

    /**
     * 测试配置中心
     */
    @Value("${coupon.user.name}")
    private String name;
    @Value("${coupon.user.age}")
    private Integer age;

    @GetMapping("/test")
    public Result<Void> test1() {
        return Result.success();
    }

    /**
     * C端-可领取优惠券列表（已发布且在领取时间窗口内，带 token 时标记已领取）
     */
    @GetMapping("/member/list")
    public Result<List<CouponEntity>> memberList(HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        return Result.success(couponService.getMemberCouponList(memberId));
    }

    /**
     * 店铺优惠券列表（已发布 + 领取时间窗口内 + 属于该店铺；带 token 时标记已领取）
     */
    @GetMapping("/shop/list")
    public Result<List<CouponEntity>> shopList(@RequestParam("brandId") Long brandId,
                                               HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        return Result.success(couponService.getShopCouponList(brandId, memberId));
    }

    /**
     * 可使用优惠券列表（已发布且在有效窗口内）；
     * 传 skuId 时按适用范围过滤（全场通用/指定分类含祖先/指定商品）——商品管理 SKU 展开区展示用
     */
    @GetMapping("/usable/list")
    public Result<List<CouponEntity>> usableList(@RequestParam(value = "skuId", required = false) Long skuId) {
        return Result.success(couponService.listUsable(skuId));
    }

    /**
     * C端-结算页可用优惠券：当前会员已领取未使用 + 门槛满足 + 适用范围匹配订单商品
     *
     * @param amount 订单商品总额
     * @param skuIds 订单商品 skuId 列表（逗号分隔）
     */
    @GetMapping("/member/usable")
    public Result<List<CouponEntity>> memberUsable(@RequestParam(value = "amount", required = false) BigDecimal amount,
                                                   @RequestParam(value = "skuIds", required = false) List<Long> skuIds,
                                                   HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        return Result.success(couponService.listMemberUsable(memberId, amount, skuIds));
    }

    /**
     * 服务间-优惠券使用校验（mall-order 提交订单时调用）：通过返回优惠金额
     */
    @PostMapping("/internal/use/check")
    public Result<BigDecimal> internalUseCheck(@RequestBody CouponUseCheckTo to) {
        return Result.success(couponService.useCheckCoupon(to));
    }

    /**
     * 服务间-下单成功核销优惠券（mall-order 调用）
     */
    @PostMapping("/internal/use/consume")
    public Result<Void> internalUseConsume(@RequestBody CouponUseCheckTo to) {
        couponService.consumeCoupon(to.getMemberId(), to.getCouponId(), to.getOrderSn());
        return Result.success();
    }

    /**
     * 服务间-订单取消/关闭回退优惠券（mall-order 调用）
     */
    @PostMapping("/internal/use/refund")
    public Result<Void> internalUseRefund(@RequestBody CouponUseCheckTo to) {
        couponService.refundCoupon(to.getMemberId(), to.getCouponId(), to.getOrderSn());
        return Result.success();
    }

    /**
     * C端-领取优惠券（写入 sms_coupon_history，领取数 +1）
     */
    @PostMapping("/member/receive/{couponId}")
    public Result<Void> receive(@PathVariable Long couponId, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            throw new RRException("请先登录");
        }
        couponService.receiveCoupon(memberId, couponId);
        return Result.success();
    }

    /**
     * 发布/下架
     */
    @PostMapping("/publish")
    public Result<Void> publish(@RequestBody CouponEntity coupon) {
        if (coupon.getId() == null) {
            throw new RRException("参数错误");
        }
        couponService.updatePublish(coupon.getId(),
                coupon.getPublish() != null ? coupon.getPublish() : 0);
        return Result.success();
    }

    /**
     * 复制优惠券
     */
    @PostMapping("/copy/{id}")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.success(couponService.copyCoupon(id));
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = couponService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<CouponEntity> info(@PathVariable Long id) {
        CouponEntity coupon = couponService.getById(id);

        return Result.success(coupon);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Long> save(@RequestBody CouponEntity coupon) {
        couponService.save(coupon);

        // 返回新券 id：前端保存「指定商品」关联（coupon_spu_relation）需要
        return Result.success(coupon.getId());
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody CouponEntity coupon) {
        couponService.updateById(coupon);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        couponService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
