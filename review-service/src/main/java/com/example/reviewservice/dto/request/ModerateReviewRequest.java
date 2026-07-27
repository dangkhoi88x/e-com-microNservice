package com.example.reviewservice.dto.request;

import com.example.reviewservice.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record ModerateReviewRequest(@NotNull ReviewStatus status) {
}
