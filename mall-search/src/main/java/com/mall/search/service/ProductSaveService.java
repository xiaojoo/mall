package com.mall.search.service;

import com.mall.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;

    /**
     * 下架：按 skuId 删除 ES 中的商品文档
     */
    boolean productDown(List<Long> skuIds) throws IOException;
}
