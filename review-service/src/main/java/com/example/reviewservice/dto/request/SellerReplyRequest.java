package com.example.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerReplyRequest(@NotBlank @Size(max = 2000) String reply) {
}
