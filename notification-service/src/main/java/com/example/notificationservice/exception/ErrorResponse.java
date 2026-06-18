package com.example.notificationservice.exception;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int code;
    private String message;
    private String error;
    private String path;
    private long timestamp;
}
