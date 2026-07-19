package com.example.microserviceecom.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    USER_EXISTED(409, "user.existed", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    MISSING_REFRESH_TOKEN(401, "Missing refresh token", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_ORIGIN(403, "Invalid refresh request origin", HttpStatus.FORBIDDEN),
    ;

    private final int code;
    private final String message;
    private final HttpStatus status;
}
