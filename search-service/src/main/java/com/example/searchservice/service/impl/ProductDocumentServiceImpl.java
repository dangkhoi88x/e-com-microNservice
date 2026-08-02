package com.example.searchservice.service.impl;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.configuration.SearchRedisCacheConfiguration;
import com.example.searchservice.dto.request.SearchRequest;
import com.example.searchservice.dto.request.SearchSort;
import com.example.searchservice.dto.response.AggregationResponse;
import com.example.searchservice.dto.response.CategoryCount;
import com.example.searchservice.dto.response.PageResponse;
import com.example.searchservice.dto.response.PriceRangeBucket;
import com.example.searchservice.dto.response.PriceStats;
import com.example.searchservice.exception.ErrorCode;
import com.example.searchservice.exception.SearchServiceException;
import com.example.searchservice.repository.ProductDocumentRepository;
import com.example.searchservice.service.ProductDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-DOCUMENT-SERVICE")
public class ProductDocumentServiceImpl implements ProductDocumentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "price", "averageRating", "reviewCount");

    private final ProductDocumentRepository productDocumentRepository;

    @Override
    @CacheEvict(cacheNames = {SearchRedisCacheConfiguration.SUGGESTIONS_CACHE, SearchRedisCacheConfiguration.AGGREGATIONS_CACHE}, allEntries = true)
    public void saveProductDocument(ProductDocument document) {
        try {
            productDocumentRepository.save(document);
            log.info("Saved product document: {}", document.getProductId());
        } catch (IOException exception) {
            log.error("Failed to save product document: {}", document.getProductId(), exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    @CacheEvict(cacheNames = {SearchRedisCacheConfiguration.SUGGESTIONS_CACHE, SearchRedisCacheConfiguration.AGGREGATIONS_CACHE}, allEntries = true)
    public void deleteProductDocument(String id) {
        try {
            productDocumentRepository.deleteById(id);
            log.info("Deleted product document: {}", id);
        } catch (IOException exception) {
            log.error("Failed to delete product document: {}", id, exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    public PageResponse<ProductDocument> getAllWithSearch(int page, int size, SearchRequest request, String sort) {
        validatePriceRange(request);
        int pageSize = normalizePageSize(size);
        SearchSort searchSort = parseSort(sort);

        try {
            return productDocumentRepository.search(page, pageSize, request, searchSort);
        } catch (IOException exception) {
            log.error("Failed to search product documents", exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    @Cacheable(cacheNames = SearchRedisCacheConfiguration.AGGREGATIONS_CACHE, key = "#request.toString()")
    public AggregationResponse getAggregations(SearchRequest request) {
        validatePriceRange(request);

        try {
            Map<String, Aggregate> aggregations = productDocumentRepository.aggregate(request);

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

    @Override
    @Cacheable(
            cacheNames = SearchRedisCacheConfiguration.SUGGESTIONS_CACHE,
            key = "#query.trim().toLowerCase() + ':' + T(java.lang.Math).min(T(java.lang.Math).max(#size, 1), 10)",
            condition = "#query != null && #query.trim().length() >= 2"
    )
    public List<ProductDocument> getSuggestions(String query, int size) {
        if (query == null || query.trim().length() < 2) return Collections.emptyList();
        try {
            return productDocumentRepository.suggestions(query.trim(), Math.min(Math.max(size, 1), 10));
        } catch (IOException exception) {
            log.error("Failed to get product suggestions", exception);
            throw new SearchServiceException(ErrorCode.ELASTICSEARCH_ERROR);
        }
    }

    @Override
    @CacheEvict(cacheNames = {SearchRedisCacheConfiguration.SUGGESTIONS_CACHE, SearchRedisCacheConfiguration.AGGREGATIONS_CACHE}, allEntries = true)
    public void updateReviewSummary(String productId, double averageRating, long reviewCount) {
        try {
            productDocumentRepository.updateReviewSummary(productId, averageRating, reviewCount);
            log.info("Updated review summary: productId={}, rating={}, count={}",
                    productId, averageRating, reviewCount);
        } catch (IOException exception) {
            log.error("Failed to update review summary: productId={}", productId, exception);
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

    private int normalizePageSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private void validatePriceRange(SearchRequest request) {
        if (request == null || request.minPrice() == null || request.maxPrice() == null) {
            return;
        }

        if (request.minPrice() > request.maxPrice()) {
            throw new SearchServiceException(ErrorCode.INVALID_SEARCH_REQUEST);
        }
    }

    private SearchSort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SearchSort("createdAt", SearchSort.Direction.DESC);
        }

        String[] parts = sort.split(",", -1);
        if (parts.length != 2) {
            throw new SearchServiceException(ErrorCode.INVALID_SEARCH_REQUEST);
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new SearchServiceException(ErrorCode.INVALID_SEARCH_REQUEST);
        }

        return switch (direction) {
            case "asc" -> new SearchSort(field, SearchSort.Direction.ASC);
            case "desc" -> new SearchSort(field, SearchSort.Direction.DESC);
            default -> throw new SearchServiceException(ErrorCode.INVALID_SEARCH_REQUEST);
        };
    }

}
