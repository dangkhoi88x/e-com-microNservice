package com.example.notificationservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {


    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    NOTIFICATION_NOT_FOUND(404, "Notification Not Found", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED(403, "Notification Access Denied", HttpStatus.FORBIDDEN),
    ;

    private final int code;
    private final String message;
    private final HttpStatus status;
}
