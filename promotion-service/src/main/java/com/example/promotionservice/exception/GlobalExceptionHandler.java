package com.example.promotionservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PromotionServiceException.class)
    ResponseEntity<ErrorResponse> handlePromotion(PromotionServiceException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return response(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(HttpServletRequest request) {
        return response(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode code, HttpServletRequest request) {
        return ResponseEntity.status(code.getHttpStatus()).body(ErrorResponse.builder()
                .code(code.getCode())
                .message(code.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build());
    }
}
