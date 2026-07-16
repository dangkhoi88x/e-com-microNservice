package com.example.microserviceecom.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IntrospecRequest {
    @NotBlank
    private String token;
}
