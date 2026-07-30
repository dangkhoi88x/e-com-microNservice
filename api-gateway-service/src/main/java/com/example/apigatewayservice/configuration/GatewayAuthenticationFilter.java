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
            new PublicEndpoint("/actuator/health/**", HttpMethod.GET),
            new PublicEndpoint("/actuator/info", HttpMethod.GET),
            new PublicEndpoint("/identity/users", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/login", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/refresh-token", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/password-reset/request", HttpMethod.POST),
            new PublicEndpoint("/identity/auth/password-reset/confirm", HttpMethod.POST),
            new PublicEndpoint("/identity/.well-known/jwks.json", HttpMethod.GET),
            new PublicEndpoint("/identity/search/**", HttpMethod.GET),  // Search API là public
            new PublicEndpoint("/api/v1/search/**", HttpMethod.GET),
            new PublicEndpoint("/search/api/v1/search/**", HttpMethod.GET),
            new PublicEndpoint("/api/v1/inventory/products/**", HttpMethod.GET),
            new PublicEndpoint("/inventory/api/v1/inventory/products/**", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/categories", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/products", HttpMethod.GET),
            new PublicEndpoint("/product/api/v1/products/**", HttpMethod.GET),
            new PublicEndpoint("/api/v1/flash-deals/live", HttpMethod.GET),
            new PublicEndpoint("/api/v1/flash-deals/active", HttpMethod.GET),
            new PublicEndpoint("/api/v1/flash-deals/long-term/live", HttpMethod.GET),
            new PublicEndpoint("/api/v1/flash-deals/upcoming", HttpMethod.GET),
            new PublicEndpoint("/api/v1/reviews/products/**", HttpMethod.GET),
            new PublicEndpoint("/api/v1/media/*/content", HttpMethod.GET),
            new PublicEndpoint("/payment/api/v1/payments/stripe/webhook", HttpMethod.POST)
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Lấy path và method từ request de get va post
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (HttpMethod.OPTIONS.equals(method)) return chain.filter(exchange);
        if(publicEndpoint(path, method)) return chain.filter(exchange); //cho request tiep tuc den filter/route tiep
        List<String> authorization = exchange.getRequest().getHeaders().get("Authorization"); // kiem tra auth
        if (authorization == null || authorization.isEmpty()) {
            return unauthenticated(exchange);
        }
        String authHeader = authorization.getFirst();
        if(!authHeader.startsWith("Bearer ")) {
            return unauthenticated(exchange);
        }
        String token = authHeader.substring(7).trim(); // lay token
        if (token.isEmpty()) {
            return unauthenticated(exchange);
        }
        //Gọi Identity Service bằng gRPC
        Mono<Boolean> tokenValidity = introspectGrpcClient.introspect(token)
                .map(response -> response.getValid())
                .onErrorResume(throwable -> {
                    log.error("Identity gRPC introspection unavailable", throwable);
                    return serviceUnavailable(exchange).then(Mono.empty());
                });
        //Cho qua hoặc trả 401
        return tokenValidity.flatMap(valid -> {
            if(valid) {
                return chain.filter(exchange);
            }
            return unauthenticated(exchange);
        });

    }
    private boolean publicEndpoint(String path, HttpMethod method) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint ->
                pathMatcher.match(endpoint.getPath(), path)
                        && (endpoint.getHttpMethod() ==    null || endpoint.getHttpMethod().equals(method))
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
    //WebFlux -> Mono<T> đại diện cho một kết quả bất đồng bộ có tối đa một giá trị -> Response 401
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
    //Response 503
    private Mono<Void> serviceUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse
                .builder()
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("AUTHENTICATION_SERVICE_UNAVAILABLE")
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .path(exchange.getRequest().getURI().getPath())
                .timestamp(System.currentTimeMillis())
                .build();

        byte[] bytes = jsonMapper.writeValueAsBytes(errorResponse);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }
}
