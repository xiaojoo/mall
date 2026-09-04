package com.mall.weapp.app;

import com.mall.common.utils.Result;
import com.mall.weapp.entity.WeappOrderCreateEntity;
import com.mall.weapp.feign.OrderFeignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序 - 订单模块控制器
 * <p>提供订单的创建、查询、取消等接口，供小程序端调用</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@RestController
@RequestMapping("weapp/order")
@RequiredArgsConstructor
public class WeappOrderController {

    private final OrderFeignService orderFeignService;

    /**
     * 查询当前用户的订单列表
     *
     * @param page  当前页码
     * @param limit 每页数量
     * @param status 订单状态（可选：0-待付款 1-已付款 2-已发货 3-已完成 4-已关闭）
     * @return 订单列表
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                               @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page.toString());
        params.put("limit", limit.toString());
        if (status != null) {
            params.put("status", status.toString());
        }
        return orderFeignService.list(params);
    }

    /**
     * 查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/info/{id}")
    public Result<Object> info(@PathVariable Long id) {
        return orderFeignService.info(id);
    }

    /**
     * 创建订单
     *
     * @param vo 创建订单参数（cartIds, addressId）
     * @return 订单ID
     */
    @PostMapping("/create")
    public Result<Object> create(@RequestBody WeappOrderCreateEntity vo) {
        return orderFeignService.createOrder(vo.getCartIds(), vo.getAddressId());
    }

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{id}")
    public Result<Object> cancel(@PathVariable Long id) {
        return orderFeignService.cancel(id);
    }
}
