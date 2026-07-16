package com.example.apigatewayservice.configuration;

import com.example.apigatewayservice.dto.ErrorResponse;
import com.example.apigatewayservice.dto.PublicEndpoint;
import com.example.apigatewayservice.grpc.IntrospectGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final IntrospectGrpcClient introspectGrpcClient;
    private final JsonMapper jsonMapper;
    // Public endpoints không cần authentication
    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint("/identity/users", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/login", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/refresh-token", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/token/introspect", HttpMethod.POST),
            new PublicEndpoint("/identity/search/**", HttpMethod.GET),  // Search API là public
            new PublicEndpoint("/api/v1/search/**", HttpMethod.GET),
            new PublicEndpoint("/search/api/v1/search/**", HttpMethod.GET),
            new PublicEndpoint("/api/v1/inventory/products/**", HttpMethod.GET),
            new PublicEndpoint("/inventory/api/v1/inventory/products/**", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/categories", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/products", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/products/**", HttpMethod.GET)
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Lấy path và method từ request
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (HttpMethod.OPTIONS.equals(method)) return chain.filter(exchange);
        if(publicEndpoint(path, method)) return chain.filter(exchange);
        List<String> authorization = exchange.getRequest().getHeaders().get("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return unauthenticated(exchange);
        }
        String authHeader = authorization.getFirst();
        if(!authHeader.startsWith("Bearer ")) {
            return unauthenticated(exchange);
        }
        String token = authHeader.substring(7);
        return introspectGrpcClient.introspect(token)
                .map(com.javabuilder.authentication.grpc.IntrospectResponse::getValid)
                .onErrorResume(throwable -> {
                    log.error("Token introspection failed", throwable);
                    return Mono.just(false);
                })
                .defaultIfEmpty(false)
                .flatMap(valid -> {
                    if(valid) {
                        return chain.filter(exchange);
                    }
                    return unauthenticated(exchange);
                });

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
    private Mono<Void> unauthenticated(ServerWebExchange exchange) {

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
