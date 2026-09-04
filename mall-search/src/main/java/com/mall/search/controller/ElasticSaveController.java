package com.mall.search.controller;

import com.mall.common.exception.BizCodeEnum;
import com.mall.common.to.es.SkuEsModel;
import com.mall.common.utils.Result;
import com.mall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Slf4j
@RestController
@RequestMapping("/search/save")
@RequiredArgsConstructor
public class ElasticSaveController {

    final ProductSaveService productSaveService;

    // 上架商品
    @PostMapping("/product")
    public Result<Object> productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
        boolean b = false;
        try {
            b = productSaveService.productStatusUp(skuEsModels);
        } catch (Exception e) {
            log.error("ElasticSaveController商品上架错误：{}", e.getMessage());
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if (!b) {
            return Result.success();
        } else {
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
    }

    // 下架商品：删除 ES 中的商品文档
    @PostMapping("/product/delete")
    public Result<Object> productDown(@RequestBody List<Long> skuIds) {
        try {
            boolean b = productSaveService.productDown(skuIds);
            if (!b) {
                return Result.success();
            }
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        } catch (Exception e) {
            log.error("ElasticSaveController商品下架错误：{}", e.getMessage());
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
    }
}
