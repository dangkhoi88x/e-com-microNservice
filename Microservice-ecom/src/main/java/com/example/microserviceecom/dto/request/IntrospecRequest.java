package com.example.microserviceecom.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class IntrospecRequest {
    @NotBlank
    private String token;
}
