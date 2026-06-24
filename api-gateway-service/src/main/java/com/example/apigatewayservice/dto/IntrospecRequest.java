package com.example.apigatewayservice.dto;



import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IntrospecRequest {

    private String token;
}
