package com.example.profileservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {


    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    USER_PROFILE_EXISTED(402, "User Profile Existed", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(404, "User Not Found", HttpStatus.NOT_FOUND),
    INVALID_USER_CREATED_EVENT(400, "User created event is invalid", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatus status;
}
