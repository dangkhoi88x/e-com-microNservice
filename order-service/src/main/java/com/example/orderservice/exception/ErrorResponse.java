package com.example.orderservice.exception;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int code;
    private String message;
    private String error;
    private String path;
    private long timestamp;
}
