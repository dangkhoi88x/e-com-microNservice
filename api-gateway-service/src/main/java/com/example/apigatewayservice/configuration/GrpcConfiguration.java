package com.example.apigatewayservice.configuration;

import com.javabuilder.authentication.grpc.IntrospectServiceGrpc;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {
    @Bean
    IntrospectServiceGrpc.IntrospectServiceFutureStub futureStub(GrpcChannelFactory channels)
    {
        return IntrospectServiceGrpc.newFutureStub(channels.createChannel("identity-service"));
    }
}
