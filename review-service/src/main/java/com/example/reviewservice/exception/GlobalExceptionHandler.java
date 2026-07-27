package com.example.reviewservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ReviewServiceException.class)
    ResponseEntity<ErrorResponse> handleReview(ReviewServiceException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest request) {
        return response(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConflict(HttpServletRequest request) {
        return response(ErrorCode.REVIEW_DATA_CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(HttpServletRequest request) {
        return response(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build());
    }
}
