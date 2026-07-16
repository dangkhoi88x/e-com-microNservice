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

@Service
@RequiredArgsConstructor
@Slf4j
public class IntrospectGrpcClient {
    private final IntrospectServiceGrpc.IntrospectServiceFutureStub futureStub;

    public Mono<IntrospectResponse> introspect(String token) {
        IntrospecRequest introspectRequest = IntrospecRequest.newBuilder()
                .setToken(token)
                .build();

        return Mono.create(sink -> Futures.addCallback(
                futureStub.introspect(introspectRequest),
                new FutureCallback<IntrospectResponse>() {
                    @Override
                    public void onSuccess(IntrospectResponse response) {
                        sink.success(response);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Token introspection failed", t);
                        sink.error(t);
                    }
                },
                MoreExecutors.directExecutor()
        ));
    }
}
