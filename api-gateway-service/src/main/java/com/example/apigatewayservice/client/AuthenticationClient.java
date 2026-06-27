package com.example.apigatewayservice.client;


import com.example.apigatewayservice.dto.ApiResponse;
import com.example.apigatewayservice.dto.IntrospecRequest;
import com.example.apigatewayservice.dto.IntrospectResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;
@HttpExchange
public interface AuthenticationClient {
    @PostExchange("/identity/api/auth/token/introspect")
    Mono<ApiResponse<IntrospectResponse>> introspection(@RequestBody @Valid IntrospecRequest request);
}
