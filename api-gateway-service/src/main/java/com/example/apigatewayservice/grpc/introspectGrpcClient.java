package com.example.apigatewayservice.grpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.javabuilder.authentication.grpc.IntrospecRequest;
import com.javabuilder.authentication.grpc.IntrospectResponse;
import com.javabuilder.authentication.grpc.IntrospectServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IntrospectGrpcClient {
    private final IntrospectServiceGrpc.IntrospectServiceFutureStub futureStub;

    @Value("${authentication.grpc-deadline-ms:800}")
    private long deadlineMs;

    public Mono<IntrospectResponse> introspect(String token) {
        return Mono.defer(() -> {
            //Tạo request gọi gRPC
            IntrospecRequest introspectRequest = IntrospecRequest.newBuilder()
                    .setToken(token)
                    .build();
            // Neu entity  service ko tra loi trong 800ms thi ket thuc
            var future = futureStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .introspect(introspectRequest);
            //Chuyển Future thành Monod
            return Mono.<IntrospectResponse>create(sink -> {
                Futures.addCallback(
                        future,
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(IntrospectResponse response) {
                                sink.success(response);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                sink.error(throwable);
                            }
                        },
                        MoreExecutors.directExecutor()
                );
                sink.onCancel(() -> future.cancel(true));
            });
        });
    }
}
