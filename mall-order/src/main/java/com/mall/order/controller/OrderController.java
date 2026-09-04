package com.mall.order.controller;

import java.util.Map;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.order.entity.OrderEntity;
import com.mall.order.service.OrderService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.common.utils.ResultCode;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;


/**
 * 订单
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:32:06
 */
@RestController
@RequestMapping({"/api/order/order", "order/order"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 远程根据订单编号查询订单状态
     */
    @GetMapping(value = "/status/{orderSn}")
    public Result<Object> getOrderStatus(@PathVariable String orderSn) {
        OrderEntity orderEntity = orderService.getOrderByOrderSn(orderSn);
        return Result.success().setData(orderEntity);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = orderService.queryPage(params);

        return Result.success(page);
    }

    /**
     * 会员界面远程访问：分页查询当前登录用户的所有订单信息
     */
    @PostMapping("/listWithItem")
    public Result<Object> listWithItem(@RequestParam Map<String, Object> params) {
        PageUtils page = orderService.queryPageWithItem(params);

        return Result.success(page);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<OrderEntity> info(@PathVariable Long id) {
        OrderEntity order = orderService.getById(id);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND.getCode(), "订单不存在或已被删除");
        }
        return Result.success(order);
    }

    /**
     * 信息（按订单号查，业务主键，管理端详情用）
     */
    @GetMapping("/infoBySn/{orderSn}")
    public Result<OrderEntity> infoBySn(@PathVariable String orderSn) {
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND.getCode(), "订单不存在或已被删除");
        }
        return Result.success(order);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody OrderEntity order) {
        orderService.save(order);

        return Result.success();
    }

    /**
     * 修改（管理端按 orderSn 更新：发货/关闭；兼容传 id 按主键更新）
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody OrderEntity order) {
        if (StringUtils.isNotBlank(order.getOrderSn())) {
            orderService.update(order, new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getOrderSn, order.getOrderSn()));
        } else {
            orderService.updateById(order);
        }

        return Result.success();
    }

    /**
     * 删除（按订单号）
     */
    @PostMapping("/deleteBySn")
    public Result<Void> deleteBySn(@RequestBody String[] orderSns) {
        orderService.remove(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getOrderSn, Arrays.asList(orderSns)));

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        orderService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
