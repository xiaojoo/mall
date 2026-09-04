package com.mall.weapp.app;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.weapp.feign.ProductFeignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序 - 商品模块控制器
 * <p>提供商品列表、详情、分类、热销、新品等接口，供小程序端调用</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@RestController
@RequestMapping("weapp/product")
@RequiredArgsConstructor
public class WeappProductController {

    private final ProductFeignService productFeignService;

    /**
     * 商品分页列表（带分类过滤）
     *
     * @param catalogId 分类ID（可选）
     * @param page      当前页码
     * @param limit     每页数量
     * @return 商品分页列表
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(value = "catalogId", required = false) Long catalogId,
                               @RequestParam(value = "page", defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page.toString());
        params.put("limit", limit.toString());
        if (catalogId != null) {
            params.put("catalogId", catalogId.toString());
        }
        return productFeignService.list(params);
    }

    /**
     * 商品详情（SPU + SKU + 品牌 + 图片）
     *
     * @param id 商品SPU ID
     * @return 商品详情
     */
    @GetMapping("/info/{id}")
    public Result<Object> info(@PathVariable Long id) {
        return productFeignService.info(id);
    }

    /**
     * 商品分类树
     *
     * @return 分类树结构
     */
    @GetMapping("/category")
    public Result<Object> category() {
        return productFeignService.categoryList();
    }

    /**
     * 热销商品列表
     *
     * @return 热销商品列表
     */
    @GetMapping("/hot")
    public Result<Object> hot() {
        Map<String, Object> params = new HashMap<>();
        params.put("sale", "1");
        params.put("limit", "10");
        return productFeignService.list(params);
    }

    /**
     * 新品上架列表
     *
     * @return 新品列表
     */
    @GetMapping("/new")
    public Result<Object> newList() {
        Map<String, Object> params = new HashMap<>();
        params.put("new", "1");
        params.put("limit", "10");
        return productFeignService.list(params);
    }
}
