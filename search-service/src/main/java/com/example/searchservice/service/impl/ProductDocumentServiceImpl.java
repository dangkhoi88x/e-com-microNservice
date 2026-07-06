package com.example.searchservice.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.request.SearchRequest;
import com.example.searchservice.dto.response.AggregationResponse;
import com.example.searchservice.dto.response.CategoryCount;
import com.example.searchservice.dto.response.PageResponse;
import com.example.searchservice.dto.response.PriceRangeBucket;
import com.example.searchservice.dto.response.PriceStats;
import com.example.searchservice.exception.ErrorCode;
import com.example.searchservice.exception.SearchServiceException;
import com.example.searchservice.service.ProductDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.example.searchservice.configuration.ElasticsearchIndexInitializer.PRODUCT_INDEX;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-DOCUMENT-SERVICE")
public class ProductDocumentServiceImpl implements ProductDocumentService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void saveProductDocument(ProductDocument document) {
        try {
            elasticsearchClient.index(i -> i
                    .index(PRODUCT_INDEX)
                    .id(document.getProductId())
                    .document(document));
            log.info("Saved product document: {}", document.getProductId());
        } catch (IOException exception) {
            log.error("Failed to save product document: {}", document.getProductId(), exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    public void deleteProductDocument(String id) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(PRODUCT_INDEX)
                    .id(id));
            log.info("Deleted product document: {}", id);
        } catch (IOException exception) {
            log.error("Failed to delete product document: {}", id, exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    public PageResponse<ProductDocument> getAllWithSearch(int page, int size, SearchRequest request) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int from = (currentPage - 1) * pageSize;

        try {
            Query query = buildSearchQuery(request);
            SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                            .index(PRODUCT_INDEX)
                            .from(from)
                            .size(pageSize)
                            .query(query),
                    ProductDocument.class);

            long totalElements = response.hits().total() == null
                    ? 0
                    : response.hits().total().value();

            List<ProductDocument> content = response.hits().hits()
                    .stream()
                    .map(Hit::source)
                    .toList();

            return PageResponse.<ProductDocument>builder()
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .totalElements(totalElements)
                    .totalPages((int) Math.ceil((double) totalElements / pageSize))
                    .content(content)
                    .build();
        } catch (IOException exception) {
            log.error("Failed to search product documents", exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    public AggregationResponse getAggregations(SearchRequest request) {
        try {
            Query query = buildSearchQuery(request);
            SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                            .index(PRODUCT_INDEX)
                            .size(0)
                            .query(query)
                            .aggregations("categories", a -> a.terms(t -> t
                                    .field("categoryName")
                                    .size(20)))
                            .aggregations("priceStats", a -> a.stats(st -> st
                                    .field("price")))
                            .aggregations("priceRanges", a -> a.range(r -> r
                                    .field("price")
                                    .ranges(range -> range.key("Dưới 1M").to(1_000_000.0))
                                    .ranges(range -> range.key("1M-5M").from(1_000_000.0).to(5_000_000.0))
                                    .ranges(range -> range.key("5M-10M").from(5_000_000.0).to(10_000_000.0))
                                    .ranges(range -> range.key("Trên 10M").from(10_000_000.0)))),
                    ProductDocument.class);

            Map<String, Aggregate> aggregations = response.aggregations();

            return AggregationResponse.builder()
                    .categories(toCategoryCounts(aggregations.get("categories")))
                    .priceStats(toPriceStats(aggregations.get("priceStats")))
                    .priceRanges(toPriceRanges(aggregations.get("priceRanges")))
                    .build();
        } catch (IOException exception) {
            log.error("Failed to aggregate product documents", exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    private List<CategoryCount> toCategoryCounts(Aggregate aggregate) {
        if (aggregate == null || !aggregate.isSterms()) {
            return Collections.emptyList();
        }

        return aggregate.sterms().buckets().array()
                .stream()
                .map(this::toCategoryCount)
                .toList();
    }

    private CategoryCount toCategoryCount(StringTermsBucket bucket) {
        return CategoryCount.builder()
                .name(bucket.key().stringValue())
                .count(bucket.docCount())
                .build();
    }

    private PriceStats toPriceStats(Aggregate aggregate) {
        if (aggregate == null || !aggregate.isStats()) {
            return PriceStats.builder()
                    .count(0)
                    .build();
        }

        StatsAggregate stats = aggregate.stats();
        return PriceStats.builder()
                .min(toBigDecimal(stats.min()))
                .max(toBigDecimal(stats.max()))
                .avg(toBigDecimal(stats.avg()))
                .count(stats.count())
                .build();
    }

    private List<PriceRangeBucket> toPriceRanges(Aggregate aggregate) {
        if (aggregate == null || !aggregate.isRange()) {
            return Collections.emptyList();
        }

        return aggregate.range().buckets().array()
                .stream()
                .map(this::toPriceRangeBucket)
                .toList();
    }

    private PriceRangeBucket toPriceRangeBucket(RangeBucket bucket) {
        return PriceRangeBucket.builder()
                .range(bucket.key())
                .count(bucket.docCount())
                .build();
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private Query buildSearchQuery(SearchRequest request) {
        if (request == null) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        if (StringUtils.hasText(request.name())) {
            mustQueries.add(buildTextSearchQuery("name", request.name()));
        }

        if (StringUtils.hasText(request.description())) {
            mustQueries.add(buildTextSearchQuery("description", request.description()));
        }

        if (StringUtils.hasText(request.categoryId())) {
            filterQueries.add(Query.of(q -> q.term(t -> t
                    .field("categoryId")
                    .value(FieldValue.of(request.categoryId())))));
        }

        if (StringUtils.hasText(request.status())) {
            filterQueries.add(Query.of(q -> q.term(t -> t
                    .field("status")
                    .value(FieldValue.of(request.status())))));
        }

        if (request.inStock() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t
                    .field("inStock")
                    .value(FieldValue.of(request.inStock())))));
        }

        if (request.minPrice() != null || request.maxPrice() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("price");
                if (request.minPrice() != null) {
                    n.gte(request.minPrice());
                }
                if (request.maxPrice() != null) {
                    n.lte(request.maxPrice());
                }
                return n;
            }))));
        }

        if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return Query.of(q -> q.bool(b -> b
                .must(mustQueries)
                .filter(filterQueries)));
    }

    private Query buildTextSearchQuery(String field, String value) {
        String keyword = value.trim();

        return Query.of(q -> q.bool(b -> b
                .should(Query.of(s -> s.match(m -> m
                        .field(field)
                        .query(keyword)
                        .fuzziness("AUTO"))))
                .should(Query.of(s -> s.matchBoolPrefix(m -> m
                        .field(field)
                        .query(keyword))))
                .minimumShouldMatch("1")));
    }
}
