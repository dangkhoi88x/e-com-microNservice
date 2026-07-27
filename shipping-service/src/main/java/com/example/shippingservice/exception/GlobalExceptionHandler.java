package com.example.shippingservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ShippingServiceException.class)
    ResponseEntity<ErrorResponse> handleShipping(
            ShippingServiceException exception,
            HttpServletRequest request
    ) {
        return response(exception.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest request) {
        return response(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConflict(HttpServletRequest request) {
        return response(ErrorCode.SHIPMENT_DATA_CONFLICT, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleConcurrentUpdate(HttpServletRequest request) {
        return response(ErrorCode.CONCURRENT_SHIPMENT_UPDATE, request);
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
