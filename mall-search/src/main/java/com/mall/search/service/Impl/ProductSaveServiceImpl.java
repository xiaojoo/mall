package com.mall.search.service.Impl;

import com.mall.common.to.es.SkuEsModel;
import com.mall.search.constant.EsConstant;
import com.mall.search.service.ProductSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSaveServiceImpl implements ProductSaveService {

    private final RestHighLevelClient client;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();

        for (SkuEsModel model : skuEsModels) {
            // 将对象转换为 JSON 字符串（使用 fastjson2）
            String json = com.alibaba.fastjson2.JSON.toJSONString(model);
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX)
                .id(model.getSkuId().toString())
                // 必须指定 XContentType，否则会走 source(Object...) 变参（要求 key-value 偶数对）导致报错
                .source(json, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        boolean hasFailures = bulk.hasFailures();

        // ES 7.x: getItems() 返回的是 BulkItemResponse[] 数组
        BulkItemResponse[] items = bulk.getItems();
        List<String> ids = new ArrayList<>();
        for (BulkItemResponse item : items) {
            ids.add(item.getId());
        }

        log.info("商品上架成功：{}", ids);
        return hasFailures;
    }

    @Override
    public boolean productDown(List<Long> skuIds) throws IOException {
        if (skuIds == null || skuIds.isEmpty()) {
            return false;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (Long skuId : skuIds) {
            bulkRequest.add(new DeleteRequest(EsConstant.PRODUCT_INDEX, skuId.toString()));
        }
        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("商品下架成功，删除sku：{}", skuIds);
        return bulk.hasFailures();
    }
}
