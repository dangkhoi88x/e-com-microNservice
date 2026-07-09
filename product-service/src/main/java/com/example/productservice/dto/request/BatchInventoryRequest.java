package com.example.productservice.dto.request;

import java.util.List;

public record BatchInventoryRequest(
        List<String> productIds
) {
}
