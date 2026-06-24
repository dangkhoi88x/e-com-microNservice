package com.example.apigatewayservice.configuration;

import com.example.apigatewayservice.client.AuthenticationClient;
import com.example.apigatewayservice.dto.ErrorResponse;
import com.example.apigatewayservice.dto.IntrospecRequest;
import com.example.apigatewayservice.dto.PublicEndpoint;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AuthenticationClient authenticationClient;
    private final JsonMapper jsonMapper;
    // Public endpoints không cần authentication
    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint("/identity/api/v1/users", HttpMethod.POST),
            new PublicEndpoint("/identity/api/v1/auth/login", HttpMethod.POST),
            new PublicEndpoint("/identity/api/v1/auth/refresh-token", HttpMethod.POST),
            new PublicEndpoint("/identity/api/v1/auth/introspect", HttpMethod.POST),
            new PublicEndpoint("/identity/api/v1/search/**", HttpMethod.GET)  // Search API là public
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Lấy path và method từ request
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if(publicEndpoint(path, method)) return chain.filter(exchange);
        List<String> authorization = exchange.getRequest().getHeaders().get("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return unauthicated(exchange);
        }
        String authHeader = authorization.getFirst();
        if(!authHeader.startsWith("Bearer ")) {
            return unauthicated(exchange);
        }
            String token = authHeader.replace("Bearer ", "");
            return authenticationClient.introspection(IntrospecRequest.builder()
                            .token(token)
                    .build()).flatMap(response ->{
                        if(response.getData().isValid()) {
                            return chain.filter(exchange);
                        }
                        else{
                            return unauthicated(exchange);
                        }

            }).onErrorResume(throwable -> unauthicated(exchange));

    }
    private boolean publicEndpoint(String path, HttpMethod method) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint ->
                pathMatcher.match(endpoint.getPath(), path)
                        && (endpoint.getHttpMethod() == null || endpoint.getHttpMethod().equals(method))
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
        private Mono<Void> unauthicated(ServerWebExchange exchange) {

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse errorResponse = ErrorResponse
                .builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .path(exchange.getRequest().getURI().getPath())
                .timestamp(System.currentTimeMillis())
                .build();
            byte[] bytes = jsonMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
}
