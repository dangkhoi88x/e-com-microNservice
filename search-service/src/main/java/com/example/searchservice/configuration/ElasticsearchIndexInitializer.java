package com.example.searchservice.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class ElasticsearchIndexInitializer {

        public static final String PRODUCT_INDEX= "products";
        private final ElasticsearchClient elasticsearchClient;


        @PostConstruct
        public void createProductIndex() throws IOException {
            boolean productsIndex = elasticsearchClient.indices().exists(p -> p.index(PRODUCT_INDEX))
                    .value();

            if (!productsIndex) {
                // Bước 2: Tạo index với mapping
                elasticsearchClient.indices().create(c -> c
                        .index(PRODUCT_INDEX)
                        .mappings(m -> m
                                // productId: keyword - exact match, không analyze
                                .properties("productId", p -> p.keyword(k -> k))

                                // name: text - full-text search, có analyze
                                .properties("name", p -> p.text(t -> t.analyzer("standard")))

                                // description: text - full-text search
                                .properties("description", p -> p.text(t -> t.analyzer("standard")))

                                // price: double - số thực, dùng cho filter/sort
                                .properties("price", p -> p.double_(d -> d))

                                // categoryId: keyword - exact match
                                .properties("categoryId", p -> p.keyword(k -> k))

                                // categoryName: keyword - exact match
                                .properties("categoryName", p -> p.keyword(k -> k))

                                // thumbnailUrl: keyword - URL, không cần search
                                .properties("thumbnailUrl", p -> p.keyword(k -> k))

                                // status: keyword - ACTIVE/INACTIVE
                                .properties("status", p -> p.keyword(k -> k))

                                // inStock: boolean - filter sản phẩm còn hàng/hết hàng
                                .properties("inStock", p -> p.boolean_(b -> b))

                                // createdAt: date - timestamp, dùng cho sort
                                .properties("createdAt", p -> p.date(d -> d))
                        )
                );
                log.info("Created index with mappings: {}", PRODUCT_INDEX);
            } else {
                log.info("Index already exists: {}", PRODUCT_INDEX);
            }
        } ;
    }
