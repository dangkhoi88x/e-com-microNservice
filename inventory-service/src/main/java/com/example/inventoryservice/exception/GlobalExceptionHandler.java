package com.example.inventoryservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<ErrorResponse> handleInventoryServiceException(
            InventoryServiceException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(buildErrorResponse(errorCode, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse response = ErrorResponse.builder()
                .code(400)
                .message(message)
                .error("Bad Request")
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while processing request: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(buildErrorResponse(errorCode, request));
    }

    private ErrorResponse buildErrorResponse(ErrorCode errorCode, HttpServletRequest request) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
