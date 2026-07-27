package com.example.sellerservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSellerShopRequest(
        @NotBlank @Size(max = 160) String shopName,
        @Size(max = 2_000) String description,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 120) String city
) {
}
