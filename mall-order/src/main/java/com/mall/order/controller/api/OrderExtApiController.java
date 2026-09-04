package com.mall.order.controller.api;

import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import com.mall.order.config.AlipayTemplate;
import com.mall.order.config.MyRabbitMQConfig;
import com.mall.order.entity.OrderEntity;
import com.mall.order.entity.OrderItemEntity;
import com.mall.order.entity.PaymentInfoEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderItemService;
import com.mall.order.service.OrderService;
import com.mall.order.service.PaymentInfoService;
import com.mall.order.vo.PayVo;
import com.mall.order.vo.RefundApplyVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 订单扩展 API 接口（前后端分离）
 * <p>
 * 替代原模板引擎页面接口：
 * <ul>
 *     <li>会员订单列表（含商品明细，对应 orderList.html / listWithItem）</li>
 *     <li>支付宝支付表单（对应 aliPayOrder 页面接口）</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderExtApiController {

    private final OrderService orderService;

    private final AlipayTemplate alipayTemplate;

    private final OrderItemService orderItemService;

    private final PaymentInfoService paymentInfoService;

    /**
     * 校验会员是否购买过某商品且已支付成功（评论前置校验，供 mall-product Feign 调用）
     *
     * @param memberId 会员 id
     * @param skuId    sku id（可为空）
     * @param spuId    spu id（可为空，skuId 与 spuId 至少一个）
     */
    @GetMapping("/paid/check")
    public Result<Boolean> paidCheck(@RequestParam("memberId") Long memberId,
                                     @RequestParam(value = "skuId", required = false) Long skuId,
                                     @RequestParam(value = "spuId", required = false) Long spuId) {
        return Result.success(orderService.hasPaidOrder(memberId, skuId, spuId));
    }

    /**
     * 申请退款（业务规则见 OrderService#refundApply）
     */
    @PostMapping("/refund/apply")
    public Result<Void> refundApply(@RequestBody RefundApplyVo vo, HttpServletRequest request) {
        orderService.refundApply(vo, request);
        return Result.success();
    }

    /**
     * 删除订单（软删除，仅限自己的订单）
     *
     * @param orderSn 订单号
     */
    @PostMapping("/delete/{orderSn}")
    public Result<Void> delete(@PathVariable String orderSn) {
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        if (member != null && !order.getMemberId().equals(member.getId())) {
            return Result.fail("无权操作该订单");
        }
        // 逻辑删除：@TableLogic 会把 remove 转为 UPDATE delete_status=1
        orderService.remove(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderSn, orderSn));
        return Result.success();
    }

    /**
     * 订单详情（含商品明细与支付信息）
     *
     * @param orderSn 订单号
     */
    @GetMapping("/detail/{orderSn}")
    public Result<Map<String, Object>> detail(@PathVariable String orderSn) {
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        // 只能查看自己的订单
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        if (member != null && !order.getMemberId().equals(member.getId())) {
            return Result.fail("无权查看该订单");
        }
        List<OrderItemEntity> items = orderItemService.list(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn));
        // 补全品牌 id（店铺跳转用），失败不阻塞详情
        orderService.fillItemBrandIds(items);
        PaymentInfoEntity payment = paymentInfoService.getOne(
                new LambdaQueryWrapper<PaymentInfoEntity>().eq(PaymentInfoEntity::getOrderSn, orderSn).last("limit 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        data.put("payment", payment);
        // 最近一笔售后申请（订单详情售后流程展示）
        data.put("refundApply", orderService.getLatestRefundApply(orderSn));
        // 支付时限（分钟）与支付截止时间戳：与延迟关单队列 TTL 一致（默认 5 分钟），
        // 前端支付页倒计时以此为准，避免前端写死 30 分钟与实际关单时间不符
        int payOvertimeMinutes = MyRabbitMQConfig.PAY_OVERTIME_MINUTES;
        data.put("payOvertimeMinutes", payOvertimeMinutes);
        if (order.getCreateTime() != null) {
            data.put("payDeadline",
                    order.getCreateTime().getTime() + payOvertimeMinutes * 60_000L);
        }
        return Result.success(data);
    }

    /**
     * 当前登录会员的订单列表（含订单商品明细）
     *
     * @param page  页码，默认 1
     * @param limit 每页条数，默认 10
     */
    @GetMapping("/listWithItem")
    public Result<Map<String, Object>> listWithItem(@RequestParam(value = "page", required = false, defaultValue = "1") String page,
                                                    @RequestParam(value = "limit", required = false, defaultValue = "10") String limit,
                                                    @RequestParam(value = "status", required = false) Integer status,
                                                    @RequestParam(value = "afterSale", required = false) Integer afterSale) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", limit);
        if (status != null) {
            params.put("status", status);
        }
        if (afterSale != null) {
            params.put("afterSale", afterSale);
        }
        PageUtils pageUtils = orderService.queryPageWithItem(params);
        // 各状态订单总数（我的订单 tab 计数用，避免只统计当前页）
        Map<String, Object> data = new HashMap<>();
        data.put("list", pageUtils.getList());
        data.put("totalCount", pageUtils.getTotalCount());
        data.put("totalPage", pageUtils.getTotalPage());
        data.put("currPage", pageUtils.getCurrPage());
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        Map<String, Long> counts = new HashMap<>();
        long afterSaleCnt = 0;
        if (member != null) {
            // 进行中的售后订单不计入状态 tab 计数（只在「售后中」tab 展示）
            List<String> activeAsSns = orderService.listActiveAfterSaleSns(member.getId());
            for (int s = 0; s <= 5; s++) {
                LambdaQueryWrapper<OrderEntity> cw = new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getMemberId, member.getId())
                        .eq(OrderEntity::getStatus, s);
                if (!activeAsSns.isEmpty()) {
                    cw.notIn(OrderEntity::getOrderSn, activeAsSns);
                }
                counts.put(String.valueOf(s), orderService.count(cw));
            }
            afterSaleCnt = orderService.countAfterSaleOrders(member.getId());
        }
        data.put("statusCounts", counts);
        data.put("afterSaleCount", afterSaleCnt);
        return Result.success(data);
    }

    /**
     * 生成支付宝支付表单（HTML），前端拿到后渲染并自动提交
     *
     * @param orderSn 订单号
     */
    @PostMapping("/pay/form")
    public Result<String> payForm(@RequestParam("orderSn") String orderSn) {
        try {
            PayVo payVo = orderService.getOrderPay(orderSn);
            String payForm = alipayTemplate.pay(payVo);
            return Result.success(payForm);
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单失败, orderSn={}", orderSn, e);
            return Result.fail("生成支付表单失败：" + e.getMessage());
        }
    }
}
