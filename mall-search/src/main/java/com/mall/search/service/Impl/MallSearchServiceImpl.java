package com.mall.search.service.Impl;

import com.mall.common.to.es.SkuEsModel;
import com.mall.common.utils.Result;
import com.mall.search.constant.EsConstant;
import com.mall.search.feign.ProductFeignService;
import com.mall.search.service.MallSearchService;
import com.mall.search.vo.CatalogVO;
import com.mall.search.vo.BrandVO;
import com.mall.search.vo.SearchParam;
import com.mall.search.vo.SearchResult;
import com.mall.search.vo.AttrResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallSearchServiceImpl implements MallSearchService {

    private final RestHighLevelClient client;

    private final ProductFeignService productFeignService;

    // 搜索列表页链接（来自 Nacos mall-search 配置）
    @Value("${mall.search.list-url:http://search.example.com/list.html?}")
    private String searchListUrl;

    @Override
    public SearchResult search(SearchParam param) {
        try {
            SearchRequest searchRequest = buildSearchRequest(param);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            return buildSearchResult(response, param);
        } catch (IOException e) {
            throw new RuntimeException("搜索失败", e);
        }
    }

    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchRequest searchRequest = new SearchRequest(EsConstant.PRODUCT_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 1.1 关键词匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2.1 三级分类 id 查询
        if (param.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // 1.2.2 品牌 id 查询
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }

        // 1.2.3 属性过滤（嵌套查询）
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            for (String attr : param.getAttrs()) {
                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValue = s[1].split(":");

                BoolQueryBuilder innerBoolQuery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("attrs.attrId", attrId))
                        .must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));

                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", innerBoolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQuery);
            }
        }

        // 1.2.4 价格区间 skuPrice=0_500 / 2000_（下界_上界，空表示不限）
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            String[] s = param.getSkuPrice().split("_");
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            if (s.length > 0 && !s[0].isEmpty()) {
                rangeQuery.gte(s[0]);
            }
            if (s.length > 1 && !s[1].isEmpty()) {
                rangeQuery.lte(s[1]);
            }
            boolQueryBuilder.filter(rangeQuery);
        }

        // 库存过滤
        if (param.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        sourceBuilder.query(boolQueryBuilder);

        // 分页
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGE_SIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGE_SIZE);

        // 排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String[] s = param.getSort().split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(new FieldSortBuilder(s[0]).order(order));
        }

        // 高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        // 聚合 - 品牌
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brandAgg);

        // 聚合 - 分类
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);

        // 聚合 - 属性（嵌套）
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1. 商品列表 — getSourceAsString() 替代废弃的 getSourceAsMap()
        List<SkuEsModel> esModels = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String sourceJson = hit.getSourceAsString();
            if (sourceJson != null) {
                SkuEsModel esModel = com.alibaba.fastjson2.JSON.parseObject(sourceJson, SkuEsModel.class);
                if (esModel != null) {
                    if (!StringUtils.isEmpty(param.getKeyword()) && hit.getHighlightFields() != null) {
                        HighlightField highlightField = hit.getHighlightFields().get("skuTitle");
                        if (highlightField != null && highlightField.getFragments().length > 0) {
                            esModel.setSkuTitle(highlightField.getFragments()[0].toString());
                        }
                    }
                    esModels.add(esModel);
                }
            }
        }
        result.setProducts(esModels);

        // 2. 属性聚合
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        Aggregations aggregations = response.getAggregations();
        if (aggregations != null) {
            Nested attrAgg = aggregations.get("attr_agg");
            if (attrAgg != null) {
                Terms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
                if (attrIdAgg != null) {
                    for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
                        SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
                        attrVo.setAttrId(((Number) bucket.getKey()).longValue());

                        Terms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
                        if (attrNameAgg != null && !attrNameAgg.getBuckets().isEmpty()) {
                            attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                        }

                        Terms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
                        if (attrValueAgg != null) {
                            List<String> attrValues = attrValueAgg.getBuckets().stream()
                                    .map(b -> b.getKeyAsString()).collect(Collectors.toList());
                            attrVo.setAttrValue(attrValues);
                        }
                        attrVos.add(attrVo);
                    }
                }
            }
        }
        result.setAttrs(attrVos);

        // 3. 品牌聚合
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        if (aggregations != null) {
            Terms brandAgg = aggregations.get("brand_agg");
            if (brandAgg != null) {
                for (Terms.Bucket bucket : brandAgg.getBuckets()) {
                    SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
                    brandVo.setBrandId(((Number) bucket.getKey()).longValue());

                    Terms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
                    if (brandNameAgg != null && !brandNameAgg.getBuckets().isEmpty()) {
                        brandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                    }

                    Terms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
                    if (brandImgAgg != null && !brandImgAgg.getBuckets().isEmpty()) {
                        brandVo.setBrandImg(brandImgAgg.getBuckets().get(0).getKeyAsString());
                    }
                    brandVos.add(brandVo);
                }
            }
        }
        result.setBrands(brandVos);

        // 4. 分类聚合
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        if (aggregations != null) {
            Terms catalogAgg = aggregations.get("catalog_agg");
            if (catalogAgg != null) {
                for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
                    SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
                    catalogVo.setCatalogId(((Number) bucket.getKey()).longValue());

                    Terms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
                    if (catalogNameAgg != null && !catalogNameAgg.getBuckets().isEmpty()) {
                        catalogVo.setCatalogName(catalogNameAgg.getBuckets().get(0).getKeyAsString());
                    }
                    catalogVos.add(catalogVo);
                }
            }
        }
        result.setCatalogs(catalogVos);

        // 5. 分页 — 用 TotalHits.value() 替代直接访问 .value
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits() != null ? hits.getTotalHits().value : 0L;
        result.setPageNum(param.getPageNum());
        result.setTotal(total);
        int totalPages = (int) (total % EsConstant.PRODUCT_PAGE_SIZE == 0 ? total / EsConstant.PRODUCT_PAGE_SIZE : total / EsConstant.PRODUCT_PAGE_SIZE + 1);
        result.setTotalPage(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 6. 面包屑导航
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                Result<AttrResponseVo> r = productFeignService.info(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 200 && r.getData() != null) {
                    navVo.setNavName(r.getData().getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink(searchListUrl + replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }

        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            Result<List<BrandVO>> r = productFeignService.infos(param.getBrandId());
            if (r.getCode() == 200 && r.getData() != null) {
                List<BrandVO> brand = r.getData();
                StringBuilder buffer = new StringBuilder();
                String replace = "";
                for (BrandVO brandVo : brand) {
                    buffer.append(brandVo.getName()).append(";");
                    replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink(searchListUrl + replace);
            }
            navs.add(navVo);
        }

        return result;
    }

    /**
     * URL 编码替换查询参数
     * 用 StandardCharsets 替代废弃的字符串编码参数
     */
    private static String replaceQueryString(SearchParam param, String value, String key) {
        // 前端（SPA）未传 _queryString 时返回空，避免 NPE；该链接仅老页面面包屑使用
        if (param.get_queryString() == null) {
            return "";
        }
        String encode = URLEncoder.encode(value, StandardCharsets.UTF_8);
        encode = encode.replace("+", "%20");
        return param.get_queryString().replace("&" + key + "=" + encode, "");
    }

    @Override
    public List<String> getSuggestions(String keyword) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getHotKeys() {
        return Arrays.asList("手机", "电脑", "耳机", "键盘", "鼠标");
    }

    @Override
    public List<CatalogVO> getCatalogs() {
        return new ArrayList<>();
    }

    @Override
    public List<BrandVO> getBrands(Long catalogId) {
        return new ArrayList<>();
    }
}
