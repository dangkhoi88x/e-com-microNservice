package com.example.microserviceecom.grpc;

import com.example.microserviceecom.service.AuthenticationService;
import com.javabuilder.authentication.grpc.IntrospecRequest;
import com.javabuilder.authentication.grpc.IntrospectResponse;
import com.javabuilder.authentication.grpc.IntrospectServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j(topic = "INTROSPECT_GRPC")
public class IntrospectGrpcService extends IntrospectServiceGrpc.IntrospectServiceImplBase {
    private final AuthenticationService authenticationService;

    @Override
    public void introspect(IntrospecRequest request, StreamObserver<IntrospectResponse> responseObserver) {
        if (request.getToken().isBlank()) {
            responseObserver.onNext(IntrospectResponse.newBuilder()
                    .setValid(false)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        try {
            var introSpectRequest = com.example.microserviceecom.dto.request.IntrospecRequest.builder()
                    .token(request.getToken())
                    .build();
            var result = authenticationService.introspect(introSpectRequest);
            var grpcResponse = IntrospectResponse.newBuilder()
                    .setUserId(result.getUserId() != null ? result.getUserId() : "")
                    .setValid(result.isValid())
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error("gRPC token introspection failed", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token introspection failed")
                    .asRuntimeException());
        }
    }
}
