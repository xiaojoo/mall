package com.mall.order.controller.api;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.order.entity.OrderEntity;
import com.mall.order.service.OrderService;
import com.mall.order.vo.OrderConfirmVo;
import com.mall.order.vo.SubmitOrderResponseVo;
import com.mall.order.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import com.mall.common.dto.PageQueryDTO;
import lombok.RequiredArgsConstructor;

/**
 * 订单 API 接口 (前后端分离)
 * 
 * @author sunxiaojie
 * @date 2024-08-01
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderApiController {
    
    private final OrderService orderService;
    
    /**
     * 获取订单列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = orderService.queryPage(params);
        return Result.success( page);
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderEntity> info(@PathVariable String orderId) {
        OrderEntity order = orderService.getById(orderId);
        return Result.success( order);
    }
    
    /**
     * 确认订单
     */
    @PostMapping("/confirm")
    public Result<OrderConfirmVo> confirm(@RequestBody OrderConfirmVo confirmVo) {
        OrderConfirmVo orderConfirmVo = null;
        try {
            orderConfirmVo = orderService.confirmOrder(confirmVo);
        } catch (Exception e) {
            return Result.fail("确认订单失败：" + e.getMessage());
        }
        return Result.success( orderConfirmVo);
    }
    
    /**
     * 提交订单
     */
    @PostMapping("/submit")
    public Result<SubmitOrderResponseVo> submit(@RequestBody OrderSubmitVo submitVo) {
        SubmitOrderResponseVo orderToken = orderService.submitOrder(submitVo);
        return Result.success( orderToken);
    }
    
    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public Result<Void> pay(@RequestParam("orderId") String orderId,
                       @RequestParam("paymentType") Integer paymentType) {
        // TODO: 实现支付逻辑
        return Result.success();
    }
    
    /**
     * 取消订单（仅待付款订单可取消；秒杀订单同步回补 Redis 秒杀库存）
     * 业务校验失败（订单不存在/无权操作/状态不可取消）由全局 RRExceptionHandler 统一处理
     */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam("orderSn") String orderSn) {
        orderService.cancelOrder(orderSn);
        return Result.success();
    }

    /**
     * 主动查询/确认支付结果（支付宝异步通知丢失时兜底，查单成功就地更新订单状态）
     *
     * @param orderSn 订单号
     * @return 当前订单状态码（1=已付款）
     */
    @PostMapping("/pay/query")
    public Result<Integer> payQuery(@RequestParam("orderSn") String orderSn) {
        return Result.success(orderService.confirmPayStatus(orderSn));
    }
    
    /**
     * 确认收货
     */
    /**
     * 确认收货（已付款订单直接完成；业务逻辑见 OrderService#receiveOrder）
     */
    @PostMapping("/receive")
    public Result<Void> receive(@RequestParam("orderSn") String orderSn, HttpServletRequest request) {
        orderService.receiveOrder(orderSn, request);
        return Result.success();
    }
    
    /**
     * 删除订单
     */
    @PostMapping("/{orderId}")
    public Result<Void> delete(@PathVariable String orderId) {
        orderService.removeById(orderId);
        return Result.success();
    }
}
