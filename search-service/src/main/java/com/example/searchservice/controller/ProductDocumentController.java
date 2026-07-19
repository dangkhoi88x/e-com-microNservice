package com.example.searchservice.controller;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.request.SearchRequest;
import com.example.searchservice.dto.response.AggregationResponse;
import com.example.searchservice.dto.response.ApiResponse;
import com.example.searchservice.dto.response.PageResponse;
import com.example.searchservice.service.ProductDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class ProductDocumentController {

    private final ProductDocumentService productDocumentService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductDocument>> searchProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        SearchRequest request = new SearchRequest(q, categoryId, name, description, minPrice, maxPrice, status, inStock);
        PageResponse<ProductDocument> data = productDocumentService.getAllWithSearch(page, size, request, sort);

        return ApiResponse.<PageResponse<ProductDocument>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/products/suggestions")
    public ApiResponse<java.util.List<ProductDocument>> suggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int size
    ) {
        return ApiResponse.<java.util.List<ProductDocument>>builder()
                .status(HttpStatus.OK.value())
                .message("Product suggestions retrieved successfully")
                .data(productDocumentService.getSuggestions(q, size))
                .build();
    }

    @GetMapping("/products/aggregations")
    public ApiResponse<AggregationResponse> getAggregations(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean inStock
    ) {
        SearchRequest request = new SearchRequest(null, categoryId, name, description, minPrice, maxPrice, status, inStock);
        AggregationResponse data = productDocumentService.getAggregations(request);

        return ApiResponse.<AggregationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Aggregations retrieved successfully")
                .data(data)
                .build();
    }
}
