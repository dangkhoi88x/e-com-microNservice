package com.example.promotionservice.exception;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ErrorResponse(int code, String message, String path, Instant timestamp) {}
