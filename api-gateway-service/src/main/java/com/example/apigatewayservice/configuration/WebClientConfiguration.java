package com.example.apigatewayservice.configuration;

import com.example.apigatewayservice.client.AuthenticationClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfiguration {

    // Tao WebClient.Builder co ho tro load balancing qua Eureka.
    @Bean
    @LoadBalanced
    WebClient.Builder builder() {
        return WebClient.builder();
    }

    // Tao AuthenticationClient de Gateway goi sang Identity Service.
    // HttpServiceProxyFactory se tu tao implementation cho interface AuthenticationClient.
    @Bean
    AuthenticationClient authenticationClient(WebClient.Builder builder) {
        WebClient webClient = builder
                // IDENTITY-SERVICE la spring.application.name cua service dang ky tren Eureka.
                .baseUrl("lb://IDENTITY-SERVICE")
                .build();

        // Gan WebClient vao HTTP interface proxy factory.
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(WebClientAdapter.create(webClient))
                .build();

        // Tao object that tu interface AuthenticationClient de inject vao filter/service khac.
        return httpServiceProxyFactory.createClient(AuthenticationClient.class);
    }
}
