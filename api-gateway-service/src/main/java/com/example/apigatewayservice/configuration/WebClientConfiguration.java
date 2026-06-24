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
    @Bean
    WebClient.Builder builder() {
        return WebClient.builder();
    }
    @Bean
    AuthenticationClient authenticationClient(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("lb://indentity-service")
                .build();

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(WebClientAdapter.create(webClient))
                .build();
        return httpServiceProxyFactory.createClient(AuthenticationClient.class);
    }
}
