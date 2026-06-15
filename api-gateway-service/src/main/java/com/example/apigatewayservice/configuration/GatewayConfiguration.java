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
                .build();
    }

}
