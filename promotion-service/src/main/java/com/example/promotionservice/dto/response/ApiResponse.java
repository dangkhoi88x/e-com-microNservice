package com.example.promotionservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
}
