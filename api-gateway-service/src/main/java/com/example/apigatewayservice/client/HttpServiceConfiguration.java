package com.example.apigatewayservice.client;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration
@ImportHttpServices(types = {AuthenticationClient.class})
public class HttpServiceConfiguration {
}
