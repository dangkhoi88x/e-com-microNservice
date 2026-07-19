package com.example.searchservice.service;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.request.SearchRequest;
import com.example.searchservice.dto.response.AggregationResponse;
import com.example.searchservice.dto.response.PageResponse;
import java.util.List;

public interface ProductDocumentService {
    void saveProductDocument(ProductDocument document);

    void deleteProductDocument(String id);

    PageResponse<ProductDocument> getAllWithSearch(int page, int size, SearchRequest request, String sort);

    AggregationResponse getAggregations(SearchRequest request);

    List<ProductDocument> getSuggestions(String query, int size);
}
