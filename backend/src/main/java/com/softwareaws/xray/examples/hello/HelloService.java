package com.softwareaws.xray.examples.hello;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class HelloService extends HelloServiceGrpc.HelloServiceImplBase {
    @Override
    public void hello(HelloServiceOuterClass.HelloRequest request,
                      StreamObserver<HelloServiceOuterClass.HelloResponse> responseObserver) {
        responseObserver.onNext(HelloServiceOuterClass.HelloResponse.newBuilder()
                                                                    .setGreeting("Hello " + request.getName() + "!")
                                                                    .build());
        responseObserver.onCompleted();
    }

    @Override
    public void fail(HelloServiceOuterClass.FailRequest request,
                     StreamObserver<HelloServiceOuterClass.FailResponse> responseObserver) {
        responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(request.getReason())
                                                                           .withCause(new IllegalStateException("cause"))));
    }

    @Override
    public void badRequest(HelloServiceOuterClass.FailRequest request,
                           StreamObserver<HelloServiceOuterClass.FailResponse> responseObserver) {
        responseObserver.onError(new StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription(request.getReason())
                                   .withCause(new IllegalArgumentException("cause"))));
    }

    @Override
    public void throttled(HelloServiceOuterClass.FailRequest request,
                          StreamObserver<HelloServiceOuterClass.FailResponse> responseObserver) {
        responseObserver.onError(new StatusRuntimeException(
            Status.RESOURCE_EXHAUSTED.withDescription(request.getReason())
                                     .withCause(new IllegalArgumentException("cause"))));
    }
}
