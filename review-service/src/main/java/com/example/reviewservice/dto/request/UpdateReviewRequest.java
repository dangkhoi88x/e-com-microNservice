package com.example.reviewservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 3000) String content,
        @Size(max = 5) List<@NotBlank String> images
) {
}
