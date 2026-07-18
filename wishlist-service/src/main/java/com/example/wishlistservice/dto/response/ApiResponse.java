package com.example.wishlistservice.dto.response;

import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> { private int status; private String message; private T data; }
