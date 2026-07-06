package com.example.searchservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),
    ELASTICSEARCH_ERROR(500, "Failed to process request with Elasticsearch", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
