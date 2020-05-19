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

package com.softwareaws.xray.examples;

import brave.grpc.GrpcTracing;
import brave.http.HttpTracing;
import brave.httpclient.TracingHttpClientBuilder;
import brave.instrumentation.awsv2.AwsSdkTracing;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.proxies.apache.http.TracedHttpClient;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import java.net.URI;
import javax.servlet.Filter;
import org.apache.http.client.HttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootApplication
public class Application {

    @Bean
    public DynamoDbClient dynamoDb(AwsSdkTracing unused) {
        var builder = DynamoDbClient.builder();
        String dynamodbEndpoint = System.getenv("DYNAMODB_ENDPOINT");
        if (dynamodbEndpoint != null) {
            builder.endpointOverride(URI.create("http://" + dynamodbEndpoint));
        }
        return builder.build();
    }

    @Bean
    public HelloServiceGrpc.HelloServiceBlockingStub helloService(GrpcTracing grpcTracing) {
        String helloServiceEndpoint = System.getenv("HELLO_SERVICE_ENDPOINT");
        if (helloServiceEndpoint == null) {
            helloServiceEndpoint = "localhost:8081";
        }
        return HelloServiceGrpc.newBlockingStub(ManagedChannelBuilder
                                                    .forTarget(helloServiceEndpoint)
                                                    .usePlaintext()
                                                    .intercept(grpcTracing.newClientInterceptor())
                                                    .build());
    }

    @Bean
    public Filter servletFilter() {
        return new AWSXRayServletFilter("OTTest");
    }

    @Bean
    public HttpClient httpClient(HttpTracing httpTracing) {
        return new TracedHttpClient(TracingHttpClientBuilder.create(httpTracing).build(), AWSXRay.getGlobalRecorder());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
