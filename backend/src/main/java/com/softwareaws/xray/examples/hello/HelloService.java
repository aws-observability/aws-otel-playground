/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
        throw new StatusRuntimeException(Status.INTERNAL.withDescription(request.getReason())
                                                        .withCause(new IllegalStateException("cause")));
    }
}
