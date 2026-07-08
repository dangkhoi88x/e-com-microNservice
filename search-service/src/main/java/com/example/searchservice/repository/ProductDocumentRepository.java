package com.example.searchservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.request.SearchRequest;
import com.example.searchservice.dto.request.SearchSort;
import com.example.searchservice.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.searchservice.configuration.ElasticsearchIndexInitializer.PRODUCT_INDEX;

@Repository
@RequiredArgsConstructor
public class ProductDocumentRepository {

    private final ElasticsearchClient elasticsearchClient;

    public void save(ProductDocument document) throws IOException {
        elasticsearchClient.index(i -> i
                .index(PRODUCT_INDEX)
                .id(document.getProductId())
                .document(document));
    }

    public void deleteById(String id) throws IOException {
        elasticsearchClient.delete(d -> d
                .index(PRODUCT_INDEX)
                .id(id));
    }

    public PageResponse<ProductDocument> search(int page, int size, SearchRequest request, SearchSort sort) throws IOException {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int from = (currentPage - 1) * pageSize;

        SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                        .index(PRODUCT_INDEX)
                        .from(from)
                        .size(pageSize)
                        .query(buildSearchQuery(request))
                        .sort(buildSort(sort)),
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
    }

    public Map<String, Aggregate> aggregate(SearchRequest request) throws IOException {
        SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                        .index(PRODUCT_INDEX)
                        .size(0)
                        .query(buildSearchQuery(request))
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

        return response.aggregations();
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

    private SortOptions buildSort(SearchSort sort) {
        SortOrder order = sort.direction() == SearchSort.Direction.ASC
                ? SortOrder.Asc
                : SortOrder.Desc;

        return SortOptions.of(s -> s.field(f -> f
                .field(sort.field())
                .order(order)));
    }
}
