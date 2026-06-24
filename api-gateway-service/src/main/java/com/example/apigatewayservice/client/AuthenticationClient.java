package com.example.apigatewayservice.client;


import com.example.apigatewayservice.dto.ApiResponse;
import com.example.apigatewayservice.dto.IntrospecRequest;
import com.example.apigatewayservice.dto.IntrospectResponse;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange(url = "${authentication.url}")
public interface AuthenticationClient {
    @PostExchange("/identity/api/v1/auth/token/introspect")
    Mono<ApiResponse<IntrospectResponse>> introspection(@RequestBody @Valid IntrospecRequest request);
}
