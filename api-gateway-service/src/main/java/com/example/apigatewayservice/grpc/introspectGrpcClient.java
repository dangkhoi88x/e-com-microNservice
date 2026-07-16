package com.example.apigatewayservice.grpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.javabuilder.authentication.grpc.IntrospecRequest;
import com.javabuilder.authentication.grpc.IntrospectResponse;
import com.javabuilder.authentication.grpc.IntrospectServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntrospectGrpcClient {
    private final IntrospectServiceGrpc.IntrospectServiceFutureStub futureStub;

    public Mono<IntrospectResponse> introspect(String token) {
        IntrospecRequest introspecRequest = IntrospecRequest.newBuilder()
                .setToken(token)
                .build();

        CompletableFuture<IntrospectResponse> future = new CompletableFuture<>();
        Futures.addCallback(
                futureStub.introspect(introspecRequest),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(IntrospectResponse introspectResponse) {
                        future.complete(introspectResponse);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Token introspection failed", t);
                        future.completeExceptionally(t);
                    }
                },
                MoreExecutors.directExecutor()
        );
        return Mono.fromFuture(future);
    }
}
