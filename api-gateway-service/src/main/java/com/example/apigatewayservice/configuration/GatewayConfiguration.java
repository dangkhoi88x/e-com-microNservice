package com.example.apigatewayservice.configuration;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-service", r -> r.path("/identity/**")
                        .filters(f -> f.stripPrefix(1)
                                .prefixPath("/identity/api"))
                        .uri("lb://IDENTITY-SERVICE"))
                .route("profile-service", r -> r.path("/profile/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PROFILE-SERVICE"))
                .route("notification-service", r -> r.path("/notification/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://NOTIFICATION-SERVICE"))
                .route("product-service", r -> r.path("/product/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PRODUCT-SERVICE"))
                .route("search-service-direct", r -> r.path("/api/v1/search/**")
                        .uri("lb://SEARCH-SERVICE"))
                .route("search-service", r -> r.path("/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SEARCH-SERVICE"))
                .route("inventory-service-direct", r -> r.path("/api/v1/inventory/**")
                        .uri("lb://INVENTORY-SERVICE"))
                .route("inventory-service", r -> r.path("/inventory/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://INVENTORY-SERVICE"))
                .route("order-service", r -> r.path("/order/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ORDER-SERVICE"))
                .route("payment-service", r -> r.path("/payment/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PAYMENT-SERVICE"))
                .build();
    }

}
