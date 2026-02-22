package com.nameless.mall.search.service.impl;

import com.nameless.mall.core.domain.Result;

import com.nameless.mall.search.api.dto.SearchRequestDTO;
import com.nameless.mall.search.api.vo.FacetItemVO;
import com.nameless.mall.search.api.vo.ProductSearchVO;
import com.nameless.mall.search.api.vo.SearchFacetsVO;
import com.nameless.mall.search.api.vo.SearchResponseVO;
import com.nameless.mall.search.entity.ProductSearch;
import com.nameless.mall.search.repository.ProductSearchRepository;
import com.nameless.mall.product.api.feign.ProductFeignClient;
import com.nameless.mall.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import com.nameless.mall.core.domain.PageResult;
import com.nameless.mall.product.api.vo.ProductListVO;
import com.nameless.mall.product.api.vo.ProductDetailVO;
import com.nameless.mall.product.api.vo.VariantVO;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜尋服務實作類
 * <p>
 * 基於 Elasticsearch 8.x 的高級搜尋引擎，支援：
 * 1. 多維度聯集/交集聚合 (Conjunctive/Disjunctive Faceting)
 * 2. 權重分層匹配策略 (Weight Boosting Strategy)
 * 3. 跨服務數據同步 (RabbitMQ/Feign Sync)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

        private final ProductSearchRepository productSearchRepository;
        private final ProductFeignClient productFeignClient;
        private final ElasticsearchOperations elasticsearchOperations;

        @Override
        public int syncAll() {
                // 1. 初始化分頁參數
                int totalSynced = 0;
                int pageNum = 1;
                int pageSize = 100;

                while (true) {
                        // 2. 呼叫 product-service 拉取商品清單（分頁）
                        log.info("【同步】拉取第 {} 頁商品清單...", pageNum);
                        Result<PageResult<ProductListVO>> result = productFeignClient.getProductList(pageNum, pageSize);

                        if (result == null || result.getData() == null || result.getData().getRecords().isEmpty()) {
                                break;
                        }

                        List<ProductListVO> products = result.getData().getRecords();

                        // 3. 批量拉取商品詳情並組裝 ES 文檔
                        List<ProductSearch> docsToSave = new ArrayList<>();
                        for (ProductListVO p : products) {
                                try {
                                        Result<ProductDetailVO> detailResult = productFeignClient
                                                        .getProductDetail(p.getId());
                                        if (detailResult == null || detailResult.getData() == null) {
                                                log.warn("【同步】商品 ID={} 無法取得詳情，跳過", p.getId());
                                                continue;
                                        }
                                        docsToSave.add(buildSearchDoc(detailResult.getData()));
                                } catch (Exception e) {
                                        log.error("【同步】商品 ID={} 處理失敗，跳過: {}", p.getId(), e.getMessage());
                                }
                        }

                        // 4. 批量寫入 ES 索引
                        if (!docsToSave.isEmpty()) {
                                productSearchRepository.saveAll(docsToSave);
                                totalSynced += docsToSave.size();
                        }

                        // 5. 判斷是否還有下一頁
                        if (products.size() < pageSize) {
                                break;
                        }
                        pageNum++;
                }

                log.info("【同步】完成！共計同步 {} 筆商品至 ES。", totalSynced);
                return totalSynced;
        }

        @Override
        public SearchResponseVO searchAdvanced(SearchRequestDTO request) {
                log.info("【搜尋】接收到請求 - 關鍵字: {}, 分類: {}, 屬性: {}",
                                request.getKeyword(), request.getCategoryId(), request.getAttrs());

                NativeQueryBuilder queryBuilder = NativeQuery.builder();
                Map<String, List<String>> attrFilters = parseAttrFilters(request.getAttrs());

                // 1. 核心查詢：負責關鍵字評分與結構化初篩 (影響評分)
                queryBuilder.withQuery(buildCoreQuery(request));

                // 2. 後置過濾：負責精準篩選 (不影響評分，用於 Facet 聯動)
                BoolQuery postFilter = buildPostFilter(request, attrFilters);
                boolean hasPostFilter = (postFilter != null);
                if (hasPostFilter) {
                        queryBuilder.withFilter(new Query(postFilter));
                }

                // 3. 分頁與排序
                int pageNum = (request.getPageNum() != null) ? request.getPageNum() : 1;
                int pageSize = (request.getPageSize() != null) ? request.getPageSize() : 20;
                queryBuilder.withPageable(PageRequest.of(pageNum - 1, pageSize));
                applySort(queryBuilder, request.getSort());

                // 4. 配置聚合 (為側邊欄篩選器準備數據)
                applyAggregations(queryBuilder, postFilter, attrFilters, request);

                // 5. 執行搜尋並封裝結果
                SearchHits<ProductSearch> searchHits = elasticsearchOperations.search(queryBuilder.build(),
                                ProductSearch.class);
                return buildResponseVO(searchHits, pageNum, pageSize);
        }

        /**
         * 解析屬性過濾字串 (格式: "顏色:紅,藍;尺寸:L")
         */
        private Map<String, List<String>> parseAttrFilters(String attrStr) {
                Map<String, List<String>> filters = new LinkedHashMap<>();
                if (attrStr == null || attrStr.isBlank()) {
                        return filters;
                }

                try {
                        String decoded;
                        try {
                                decoded = URLDecoder.decode(attrStr, StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException e) {
                                decoded = attrStr;
                                log.warn("【搜尋】屬性參數解碼失敗，使用原始字串: {}", attrStr);
                        }

                        for (String session : decoded.split(";")) {
                                String[] parts = session.split(":");
                                if (parts.length == 2) {
                                        List<String> values = Arrays.asList(parts[1].split(","));
                                        filters.put(parts[0].trim(), values);
                                }
                        }
                } catch (Exception e) {
                        log.warn("【搜尋】屬性參數解析異常: {}", attrStr);
                }
                return filters;
        }

        /**
         * 構建核心查詢 (影響搜尋相關性評分)
         */
        private Query buildCoreQuery(SearchRequestDTO request) {
                BoolQuery.Builder bool = new BoolQuery.Builder();

                // 關鍵字分層權重策略
                if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                        String kw = request.getKeyword();

                        // 基礎匹配：品名權重提升
                        Query multiMatchBase = Query.of(q -> q.multiMatch(mm -> mm
                                        .query(kw)
                                        .fields("name^5", "title^2")
                                        .minimumShouldMatch("3<75%")
                                        .operator(Operator.And)));
                        bool.must(multiMatchBase);

                        // 精準短語加分
                        Query phraseMatch = Query.of(q -> q.matchPhrase(mp -> mp
                                        .field("name")
                                        .query(kw)
                                        .slop(2)
                                        .boost(15.0f)));
                        bool.should(phraseMatch);

                        // 廣度語意補充
                        Query multiMatchBroad = Query.of(q -> q.multiMatch(mm -> mm
                                        .query(kw)
                                        .fields("tags^2", "description^0.5")
                                        .boost(0.5f)));
                        bool.should(multiMatchBroad);
                }

                // 分類過濾
                if (request.getCategoryId() != null) {
                        long categoryId = request.getCategoryId();
                        Query categoryFilter = Query
                                        .of(q -> q.term(t -> t.field("categoryHierarchy").value(categoryId)));
                        bool.filter(categoryFilter);
                }

                // 庫存過濾：僅顯示在售商品
                Query stockFilter = Query.of(q -> q.range(r -> r.field("stock").gt(JsonData.of(0))));
                bool.filter(stockFilter);

                return new Query(bool.build());
        }

        /**
         * 構建後置過濾器 (不影響評分，用於 Conjunctive Faceting)
         */
        private BoolQuery buildPostFilter(SearchRequestDTO request, Map<String, List<String>> attrFilters) {
                BoolQuery.Builder post = new BoolQuery.Builder();
                boolean hasFilter = false;

                // 標籤篩選 (支援多選 OR)
                if (request.getTag() != null && !request.getTag().isBlank()) {
                        List<String> tags = Arrays.stream(request.getTag().split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());
                        if (!tags.isEmpty()) {
                                List<FieldValue> tagValues = tags.stream().map(FieldValue::of)
                                                .collect(Collectors.toList());
                                Query tagsFilter = Query.of(
                                                q -> q.terms(ts -> ts.field("tags").terms(t -> t.value(tagValues))));
                                post.filter(tagsFilter);
                                hasFilter = true;
                        }
                }

                // 價格區間篩選
                if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                        BigDecimal min = request.getMinPrice();
                        BigDecimal max = request.getMaxPrice();

                        // 防呆：若 min > max 則自動互換
                        if (min != null && max != null && min.compareTo(max) > 0) {
                                BigDecimal temp = min;
                                min = max;
                                max = temp;
                        }

                        BigDecimal finalMin = min;
                        BigDecimal finalMax = max;

                        Query priceFilter = Query.of(q -> q.range(r -> {
                                r.field("price");
                                if (finalMin != null) {
                                        r.gte(JsonData.of(finalMin));
                                }
                                if (finalMax != null) {
                                        r.lte(JsonData.of(finalMax));
                                }
                                return r;
                        }));
                        post.filter(priceFilter);
                        hasFilter = true;
                }

                // 屬性篩選 (Nested)
                for (Map.Entry<String, List<String>> entry : attrFilters.entrySet()) {
                        // 建立屬性名的精準匹配
                        String attrName = entry.getKey();
                        Query nameTermQuery = Query.of(q -> q.term(t -> t.field("attrs.attrName").value(attrName)));

                        // 建立屬性值的多選匹配
                        List<FieldValue> attrValues = entry.getValue().stream().map(FieldValue::of)
                                        .collect(Collectors.toList());
                        Query valueTermsQuery = Query.of(q -> q
                                        .terms(ts -> ts.field("attrs.attrValue").terms(t2 -> t2.value(attrValues))));

                        // 組合為 Bool Query (Must: 某個 Nested 物件必須同時匹配名稱與值)
                        Query boolQuery = Query.of(q -> q.bool(b -> b.must(nameTermQuery).must(valueTermsQuery)));

                        // 包裝為 Nested Query
                        Query nestedQuery = Query.of(q -> q.nested(n -> n.path("attrs").query(boolQuery)));

                        post.filter(nestedQuery);
                        hasFilter = true;
                }

                return hasFilter ? post.build() : null;
        }

        /**
         * 應用排序邏輯
         */
        private void applySort(NativeQueryBuilder builder, String sort) {
                if (sort == null) {
                        return;
                }
                switch (sort) {
                        case "price_asc":
                                builder.withSort(Sort.by(Sort.Order.asc("price")));
                                break;
                        case "price_desc":
                                builder.withSort(Sort.by(Sort.Order.desc("price")));
                                break;
                        case "sales_desc":
                                builder.withSort(Sort.by(Sort.Order.desc("salesCount")));
                                break;
                        case "newest":
                                builder.withSort(Sort.by(Sort.Order.desc("publishedAt")));
                                break;
                        default:
                                break;
                }
        }

        /**
         * 配置聚合分析 (Faceting)
         * 採用 Disjunctive Faceting 策略：每個維度的聚合排除自身過濾
         */
        private void applyAggregations(NativeQueryBuilder builder, BoolQuery postFilter,
                        Map<String, List<String>> attrFilters, SearchRequestDTO request) {

                // 1. 價格區間統計 + 分類聚合 (Conjunctive — 套用完整 post-filter 確保篩選後的精確計數)
                Aggregation priceStatsAgg = Aggregation.of(a -> a.stats(s -> s.field("price")));
                Aggregation categoriesTermsAgg = Aggregation
                                .of(a -> a.terms(t -> t.field("categoryHierarchy").size(100)));

                if (postFilter != null) {
                        Query filterQuery = new Query(postFilter);
                        Aggregation priceAggWithFilter = Aggregation
                                        .of(a -> a.filter(filterQuery).aggregations("filtered_price", priceStatsAgg));
                        Aggregation catAggWithFilter = Aggregation.of(
                                        a -> a.filter(filterQuery).aggregations("filtered_cats", categoriesTermsAgg));

                        builder.withAggregation("price_stats_agg", priceAggWithFilter);
                        builder.withAggregation("categories_agg", catAggWithFilter);
                } else {
                        builder.withAggregation("price_stats_agg", priceStatsAgg);
                        builder.withAggregation("categories_agg", categoriesTermsAgg);
                }

                // 2. 標籤聚合 (Disjunctive — 排除自身標籤過濾，使已選標籤仍顯示其他選項計數)
                Query tagDisjunctiveFilter = buildTagDisjunctiveFilter(request, attrFilters);
                Aggregation tagsTermsAgg = Aggregation.of(a -> a.terms(t -> t.field("tags").size(50)));
                Aggregation tagsAggWithFilter = Aggregation
                                .of(a -> a.filter(tagDisjunctiveFilter).aggregations("filtered_tags", tagsTermsAgg));
                builder.withAggregation("tags_disjunctive_agg", tagsAggWithFilter);

                // 3. 屬性聚合基礎層 (Conjunctive — 套用完整過濾，為「未選擇」的屬性維度提供精確計數)
                Aggregation nestedAttrAggBase = buildNestedAttrAgg();
                if (postFilter != null) {
                        Query filterQuery = new Query(postFilter);
                        Aggregation allAttrsAggWithFilter = Aggregation
                                        .of(a -> a.filter(filterQuery).aggregations("nested_attrs", nestedAttrAggBase));
                        builder.withAggregation("all_attrs_agg", allAttrsAggWithFilter);
                } else {
                        builder.withAggregation("all_attrs_agg", nestedAttrAggBase);
                }

                // 4. 屬性聚合 Disjunctive 層（為每個「已選擇」的屬性維度排除自身過濾，讓使用者可繼續多選）
                for (String currentAttr : attrFilters.keySet()) {
                        applyAttrDisjunctiveAgg(builder, currentAttr, attrFilters, request);
                }
        }

        /**
         * 構建標籤維度的 Disjunctive 過濾器 (排除標籤過濾，保留其他過濾)
         */
        private Query buildTagDisjunctiveFilter(SearchRequestDTO request, Map<String, List<String>> attrFilters) {
                BoolQuery.Builder bool = new BoolQuery.Builder();

                // 繼承核心查詢
                bool.must(buildCoreQuery(request));

                // 保留價格過濾 (自動互換)
                if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                        BigDecimal min = request.getMinPrice();
                        BigDecimal max = request.getMaxPrice();

                        if (min != null && max != null && min.compareTo(max) > 0) {
                                BigDecimal temp = min;
                                min = max;
                                max = temp;
                        }

                        BigDecimal finalMin = min;
                        BigDecimal finalMax = max;

                        Query priceFilter = Query.of(q -> q.range(r -> {
                                r.field("price");
                                if (finalMin != null) {
                                        r.gte(JsonData.of(finalMin));
                                }
                                if (finalMax != null) {
                                        r.lte(JsonData.of(finalMax));
                                }
                                return r;
                        }));
                        bool.filter(priceFilter);
                }

                // 保留所有屬性過濾
                for (Map.Entry<String, List<String>> entry : attrFilters.entrySet()) {
                        String attrName = entry.getKey();
                        Query nameTermQuery = Query.of(q -> q.term(t -> t.field("attrs.attrName").value(attrName)));
                        List<FieldValue> attrValues = entry.getValue().stream().map(FieldValue::of)
                                        .collect(Collectors.toList());
                        Query valueTermsQuery = Query.of(q -> q
                                        .terms(ts -> ts.field("attrs.attrValue").terms(t2 -> t2.value(attrValues))));
                        Query boolQuery = Query.of(q -> q.bool(b -> b.must(nameTermQuery).must(valueTermsQuery)));
                        Query nestedQuery = Query.of(q -> q.nested(n -> n.path("attrs").query(boolQuery)));
                        bool.filter(nestedQuery);
                }

                // 排除標籤過濾 (不添加 tag filter)
                return new Query(bool.build());
        }

        private Aggregation buildNestedAttrAgg() {
                Aggregation valueTermsAgg = Aggregation.of(a -> a.terms(t -> t.field("attrs.attrValue").size(50)));
                Aggregation nameTermsAgg = Aggregation.of(a -> a.terms(t -> t.field("attrs.attrName").size(20))
                                .aggregations("attr_value_agg", valueTermsAgg));
                return Aggregation.of(a -> a.nested(n -> n.path("attrs")).aggregations("attr_name_agg", nameTermsAgg));
        }

        private void applyAttrDisjunctiveAgg(NativeQueryBuilder builder, String currentAttr,
                        Map<String, List<String>> allFilters, SearchRequestDTO request) {
                BoolQuery.Builder bool = new BoolQuery.Builder();

                // 繼承核心查詢
                bool.must(buildCoreQuery(request));

                // 保留標籤過濾 (支援多選 OR)
                if (request.getTag() != null && !request.getTag().isBlank()) {
                        List<String> tags = Arrays.stream(request.getTag().split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());
                        if (!tags.isEmpty()) {
                                List<FieldValue> tagValues = tags.stream().map(FieldValue::of)
                                                .collect(Collectors.toList());
                                Query tagsFilter = Query.of(
                                                q -> q.terms(ts -> ts.field("tags").terms(t -> t.value(tagValues))));
                                bool.filter(tagsFilter);
                        }
                }

                // 價格過濾 (自動互換)
                if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                        BigDecimal min = request.getMinPrice();
                        BigDecimal max = request.getMaxPrice();

                        if (min != null && max != null && min.compareTo(max) > 0) {
                                BigDecimal temp = min;
                                min = max;
                                max = temp;
                        }

                        BigDecimal finalMin = min;
                        BigDecimal finalMax = max;

                        Query priceFilter = Query.of(q -> q.range(r -> {
                                r.field("price");
                                if (finalMin != null) {
                                        r.gte(JsonData.of(finalMin));
                                }
                                if (finalMax != null) {
                                        r.lte(JsonData.of(finalMax));
                                }
                                return r;
                        }));
                        bool.filter(priceFilter);
                }

                // 排除當前維度，保留其他屬性維度過濾 (Disjunctive 核心)
                allFilters.forEach((name, values) -> {
                        if (!name.equals(currentAttr)) {
                                Query nameTermQuery = Query.of(q -> q.term(t -> t.field("attrs.attrName").value(name)));
                                List<FieldValue> attrValues = values.stream().map(FieldValue::of)
                                                .collect(Collectors.toList());
                                Query valueTermsQuery = Query.of(q -> q.terms(
                                                ts -> ts.field("attrs.attrValue").terms(t2 -> t2.value(attrValues))));
                                Query boolQuery = Query
                                                .of(q -> q.bool(b -> b.must(nameTermQuery).must(valueTermsQuery)));
                                Query nestedQuery = Query.of(q -> q.nested(n -> n.path("attrs").query(boolQuery)));
                                bool.filter(nestedQuery);
                        }
                });

                Query filterQuery = new Query(bool.build());

                // 拆解深層巢狀 Aggregation
                Aggregation attrValuesAgg = Aggregation.of(v -> v.terms(vt -> vt.field("attrs.attrValue").size(50)));
                Query attrNameFilter = Query.of(ff -> ff.term(t -> t.field("attrs.attrName").value(currentAttr)));
                Aggregation filteredAttrAgg = Aggregation
                                .of(f -> f.filter(attrNameFilter).aggregations("attr_values", attrValuesAgg));
                Aggregation nestedAttrsAgg = Aggregation.of(
                                n -> n.nested(nn -> nn.path("attrs")).aggregations("filtered_attr", filteredAttrAgg));

                String aggName = "disjunctive_" + currentAttr + "_agg";
                Aggregation disjunctiveAgg = Aggregation
                                .of(a -> a.filter(filterQuery).aggregations("nested_attrs", nestedAttrsAgg));
                builder.withAggregation(aggName, disjunctiveAgg);
        }

        private SearchResponseVO buildResponseVO(SearchHits<ProductSearch> searchHits, int pageNum, int pageSize) {
                SearchResponseVO vo = new SearchResponseVO();
                // 1. 設定分頁資訊
                vo.setPageNum(pageNum);
                vo.setTotal(searchHits.getTotalHits());
                vo.setTotalPages((int) Math.ceil((double) searchHits.getTotalHits() / pageSize));
                // 2. 將 ES 命中結果轉換為 VO 列表
                List<ProductSearchVO> productList = new ArrayList<>();
                for (var hit : searchHits.getSearchHits()) {
                        productList.add(convertToVO(hit.getContent()));
                }
                vo.setProducts(productList);
                // 3. 解析聚合結果並附加 Facet 篩選資訊
                vo.setFacets(parseFacets(searchHits));
                return vo;
        }

        /**
         * 解析 ES 聚合結果 (Disjunctive Faceting)
         */
        private SearchFacetsVO parseFacets(SearchHits<ProductSearch> searchHits) {
                // 1. 初始化 Facet 容器
                SearchFacetsVO facets = new SearchFacetsVO();
                if (searchHits.getAggregations() == null) {
                        return facets;
                }

                // 2. 取得 ES 聚合結果並準備屬性 Disjunctive 計數表
                ElasticsearchAggregations container = (ElasticsearchAggregations) searchHits.getAggregations();
                Map<String, Map<String, Long>> attrDisjunctiveCounts = new HashMap<>();

                // 3. 逐一解析各聚合桶（分類、標籤、價格、屬性）
                for (ElasticsearchAggregation agg : container.aggregations()) {
                        try {
                                String name = agg.aggregation().getName();
                                Aggregate variant = agg.aggregation().getAggregate();
                                if (variant == null) {
                                        continue;
                                }

                                if ("tags_disjunctive_agg".equals(name)) {
                                        parseTagFacet(facets, variant);
                                } else if ("categories_agg".equals(name)) {
                                        parseCategoryFacet(facets, variant);
                                } else if ("price_stats_agg".equals(name)) {
                                        parsePriceFacet(facets, variant);
                                } else if (name.startsWith("disjunctive_") && name.endsWith("_agg")) {
                                        parseAttrDisjunctiveItem(attrDisjunctiveCounts, name, variant);
                                }

                        } catch (Exception e) {
                                log.warn("聚合項解析失敗: {} - {}", agg.aggregation().getName(), e.getMessage());
                        }
                }

                // 4. 合併屬性聚合並套用 Disjunctive 計數覆蓋
                parseAllAttrsFacet(facets, container, attrDisjunctiveCounts);
                return facets;
        }

        private void parseCategoryFacet(SearchFacetsVO facets, Aggregate variant) {
                LongTermsAggregate terms = null;
                if (variant.isFilter() && variant.filter().aggregations().containsKey("filtered_cats")) {
                        var sub = variant.filter().aggregations().get("filtered_cats");
                        if (sub.isLterms()) {
                                terms = sub.lterms();
                        }
                } else if (variant.isLterms()) {
                        terms = variant.lterms();
                }

                if (terms == null || terms.buckets() == null) {
                        return;
                }

                List<FacetItemVO> categoryList = new ArrayList<>();
                for (var bucket : terms.buckets().array()) {
                        categoryList.add(FacetItemVO.builder()
                                        .id(bucket.key())
                                        .count(bucket.docCount())
                                        .build());
                }
                facets.setCategories(categoryList);
        }

        private void parseTagFacet(SearchFacetsVO facets, Aggregate variant) {
                StringTermsAggregate terms = null;
                if (variant.isFilter() && variant.filter().aggregations().containsKey("filtered_tags")) {
                        var sub = variant.filter().aggregations().get("filtered_tags");
                        if (sub.isSterms()) {
                                terms = sub.sterms();
                        }
                } else if (variant.isSterms()) {
                        terms = variant.sterms();
                }

                if (terms == null || terms.buckets() == null) {
                        return;
                }

                List<FacetItemVO> tagList = new ArrayList<>();
                for (var bucket : terms.buckets().array()) {
                        tagList.add(FacetItemVO.builder()
                                        .name(bucket.key().stringValue())
                                        .count(bucket.docCount())
                                        .build());
                }
                facets.setTags(tagList);
        }

        private void parsePriceFacet(SearchFacetsVO facets, Aggregate variant) {
                StatsAggregate stats = null;
                if (variant.isFilter() && variant.filter().aggregations().containsKey("filtered_price")) {
                        var sub = variant.filter().aggregations().get("filtered_price");
                        if (sub.isStats()) {
                                stats = sub.stats();
                        }
                } else if (variant.isStats()) {
                        stats = variant.stats();
                }

                if (stats != null) {
                        SearchFacetsVO.PriceStats p = new SearchFacetsVO.PriceStats();
                        p.setMin(BigDecimal.valueOf(stats.min() == Double.POSITIVE_INFINITY ? 0 : stats.min()));
                        p.setMax(BigDecimal.valueOf(stats.max() == Double.NEGATIVE_INFINITY ? 0 : stats.max()));
                        p.setCount(stats.count());
                        facets.setPriceStatistics(p);
                }
        }

        private void parseAttrDisjunctiveItem(Map<String, Map<String, Long>> counts, String name, Aggregate variant) {
                String attrName = name.substring("disjunctive_".length(), name.length() - "_agg".length());
                try {
                        var nestedAttrs = variant.filter().aggregations().get("nested_attrs");
                        var filteredAttr = nestedAttrs.nested().aggregations().get("filtered_attr");
                        var attrValuesBucket = filteredAttr.filter().aggregations().get("attr_values");
                        StringTermsAggregate terms = attrValuesBucket.sterms();

                        Map<String, Long> valueMap = new HashMap<>();
                        for (var bucket : terms.buckets().array()) {
                                valueMap.put(bucket.key().stringValue(), bucket.docCount());
                        }
                        counts.put(attrName, valueMap);
                } catch (Exception e) {
                        log.debug("跳過無法解析的屬性聚合桶: {}", name, e);
                }
        }

        private void parseAllAttrsFacet(SearchFacetsVO facets, ElasticsearchAggregations container,
                        Map<String, Map<String, Long>> disjunctiveCounts) {
                try {
                        ElasticsearchAggregation allAgg = container.get("all_attrs_agg");
                        if (allAgg == null) {
                                return;
                        }

                        Aggregate variant = allAgg.aggregation().getAggregate();

                        // 1. 提取屬性名稱聚合桶
                        StringTermsAggregate attrNames = extractAttrNamesAggregate(variant);
                        if (attrNames == null || attrNames.buckets() == null) {
                                return;
                        }

                        // 2. 遍歷每個屬性名稱，組裝 Facet
                        List<SearchFacetsVO.AttributeFacet> facetsList = new ArrayList<>();

                        for (var nameBucket : attrNames.buckets().array()) {
                                String currentAttrName = nameBucket.key().stringValue();
                                Map<String, Long> currentDisjCounts = disjunctiveCounts.get(currentAttrName);

                                // 3. 提取屬性值聚合
                                if (!nameBucket.aggregations().containsKey("attr_value_agg")) {
                                        continue;
                                }
                                var valAgg = nameBucket.aggregations().get("attr_value_agg");
                                if (!valAgg.isSterms()) {
                                        continue;
                                }

                                // 4. 映射屬性值，套用 Disjunctive 計數覆蓋
                                List<SearchFacetsVO.AttrValueVO> mappedValues = new ArrayList<>();
                                for (var valueBucket : valAgg.sterms().buckets().array()) {
                                        String attrValue = valueBucket.key().stringValue();
                                        long count = valueBucket.docCount();

                                        if (currentDisjCounts != null && currentDisjCounts.containsKey(attrValue)) {
                                                count = currentDisjCounts.get(attrValue);
                                        }

                                        mappedValues.add(SearchFacetsVO.AttrValueVO.builder()
                                                        .value(attrValue)
                                                        .count(count)
                                                        .build());
                                }

                                // 5. 組裝單一屬性 Facet
                                SearchFacetsVO.AttributeFacet facet = new SearchFacetsVO.AttributeFacet();
                                facet.setAttrName(currentAttrName);
                                facet.setAttrValues(mappedValues);
                                facetsList.add(facet);
                        }

                        facets.setAttributes(facetsList);

                } catch (Exception e) {
                        log.warn("【搜尋】全屬性解析錯誤: {}", e.getMessage());
                }
        }

        /**
         * 輔助方法：展平提取 Nested 聚合中的 StringTermsAggregate
         */
        private StringTermsAggregate extractAttrNamesAggregate(Aggregate variant) {
                if (variant.isFilter() && variant.filter().aggregations().containsKey("nested_attrs")) {
                        var nested = variant.filter().aggregations().get("nested_attrs");
                        if (nested.isNested() && nested.nested().aggregations().containsKey("attr_name_agg")) {
                                var terms = nested.nested().aggregations().get("attr_name_agg");
                                if (terms.isSterms()) {
                                        return terms.sterms();
                                }
                        }
                } else if (variant.isNested() && variant.nested().aggregations().containsKey("attr_name_agg")) {
                        var terms = variant.nested().aggregations().get("attr_name_agg");
                        if (terms.isSterms()) {
                                return terms.sterms();
                        }
                }
                return null;
        }

        private ProductSearchVO convertToVO(ProductSearch entity) {
                // 1. 複製基礎屬性至 VO
                ProductSearchVO vo = new ProductSearchVO();
                BeanUtils.copyProperties(entity, vo);
                // 2. 補充顯示用欄位（標題、主圖、SKU、品牌）
                vo.setTitle(entity.getTitle());
                vo.setMainImage(entity.getMainImage());
                vo.setSkus(entity.getSkus());
                vo.setBrandName(entity.getBrandName());
                return vo;
        }

        @Override
        public void syncOne(Long productId) {
                log.info("【同步】開始同步單筆商品: ID={}", productId);
                try {
                        Result<ProductDetailVO> result = productFeignClient.getProductDetail(productId);
                        if (result == null || result.getData() == null) {
                                log.warn("【同步】未獲取到詳情，執行 ES 刪除: ID={}", productId);
                                this.deleteOne(productId);
                                return;
                        }

                        ProductSearch doc = buildSearchDoc(result.getData());
                        productSearchRepository.save(doc);
                        log.info("【同步】商品同步成功: ID={}", productId);
                } catch (Exception e) {
                        log.error("【同步】同步失敗 ID={}: {}", productId, e.getMessage());
                        throw e;
                }
        }

        /**
         * 將 ProductDetailVO 轉換為 ES 索引文檔。
         * syncAll() 批量同步和 syncOne() 單筆同步共用此方法。
         */
        private ProductSearch buildSearchDoc(ProductDetailVO dto) {
                ProductSearch doc = new ProductSearch();
                BeanUtils.copyProperties(dto, doc);
                doc.setSalesCount(dto.getSales());
                doc.setStock(dto.getStock());
                doc.setOriginalPrice(dto.getOriginalPrice());
                doc.setTitle(dto.getTitle());
                doc.setMainImage(dto.getMainImage());
                doc.setCategoryHierarchy(dto.getCategoryHierarchy());

                if (dto.getVariants() != null) {
                        List<String> skuNames = new ArrayList<>();
                        for (VariantVO v : dto.getVariants()) {
                                skuNames.add(v.getName());
                        }
                        doc.setSkus(skuNames);
                }

                if (dto.getDisplayOptions() != null) {
                        List<ProductSearch.AttributeValue> attrs = new ArrayList<>();
                        dto.getDisplayOptions().forEach((name, values) -> {
                                for (String val : values) {
                                        ProductSearch.AttributeValue attr = new ProductSearch.AttributeValue();
                                        attr.setAttrName(name);
                                        attr.setAttrValue(val);
                                        attrs.add(attr);
                                }
                        });
                        doc.setAttrs(attrs);
                }

                return doc;
        }

        @Override
        public void deleteOne(Long productId) {
                log.info("【同步】執行索引刪除: ID={}", productId);
                productSearchRepository.deleteById(productId);
                log.info("【同步】索引刪除成功: ID={}", productId);
        }
}
