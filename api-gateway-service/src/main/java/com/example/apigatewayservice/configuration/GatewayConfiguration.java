package com.example.apigatewayservice.configuration;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of truth for gateway routes.
 *
 * <p>Routes used to be split between this class and {@code application.yaml}, which silently
 * duplicated four paths. The YAML copy of {@code /order/**} was missing {@code stripPrefix(1)},
 * so whichever definition won decided whether ordering worked at all. Add new routes here only.
 */
@Configuration
public class GatewayConfiguration {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("cart-service", r -> r.path("/api/v1/cart/**")
                        .uri("lb://CART-SERVICE"))
                .route("wishlist-service", r -> r.path("/api/v1/wishlist/**")
                        .uri("lb://WISHLIST-SERVICE"))
                .route("media-service", r -> r.path("/api/v1/media/**")
                        .uri("lb://MEDIA-SERVICE"))
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
                .route("seller-product-service", r -> r.path("/api/v1/seller/products/**")
                        .uri("lb://PRODUCT-SERVICE"))
                .route("admin-product-service", r -> r.path("/api/v1/admin/products/**")
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
                .route("promotion-service", r -> r.path("/api/v1/promotions/**")
                        .uri("lb://PROMOTION-SERVICE"))
                .route("flash-deal-service", r -> r.path("/api/v1/flash-deals/**")
                        .uri("lb://PROMOTION-SERVICE"))
                .route("shipping-service", r -> r.path("/api/v1/shipments/**")
                        .uri("lb://SHIPPING-SERVICE"))
                .route("review-service", r -> r.path("/api/v1/reviews/**")
                        .uri("lb://REVIEW-SERVICE"))
                .route("seller-service", r -> r.path("/api/v1/sellers/**")
                        .uri("lb://SELLER-SERVICE"))
                .build();
    }

}
