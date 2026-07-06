package com.example.searchservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SearchServiceException.class)
    public ResponseEntity<ErrorResponse> handleSearchServiceException(SearchServiceException exception,
                                                                      WebRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(buildErrorResponse(errorCode, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, WebRequest request) {
        log.error("Unexpected error", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(buildErrorResponse(errorCode, request));
    }

    private ErrorResponse buildErrorResponse(ErrorCode errorCode, WebRequest request) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .path(extractPath(request))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
