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

import brave.Tracing;
import brave.grpc.GrpcTracing;
import brave.http.HttpTracing;
import brave.httpclient.TracingHttpClientBuilder;
import brave.instrumentation.awsv2.AwsSdkTracing;
import brave.okhttp3.TracingCallFactory;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.proxies.apache.http.TracedHttpClient;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.BraveTracing;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import java.net.URI;
import java.util.Optional;
import javax.servlet.Filter;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.apache.http.client.HttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootApplication
public class Application {

    private static final boolean ENABLE_XRAY_SDK = "true".equals(System.getenv("ENABLE_XRAY_SDK"));

    @Bean
    public DynamoDbClient dynamoDb(AwsSdkTracing awsSdkTracing) {
        var builder = DynamoDbClient.builder();
        String dynamodbEndpoint = System.getenv("DYNAMODB_ENDPOINT");
        if (dynamodbEndpoint != null) {
            builder.endpointOverride(URI.create("http://" + dynamodbEndpoint));
        }
        builder.overrideConfiguration(
            configuration -> configuration.addExecutionInterceptor(awsSdkTracing.executionInterceptor()));
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
    public Optional<Filter> servletFilter() {
        if (ENABLE_XRAY_SDK) {
            return Optional.of(new AWSXRayServletFilter("OTTest"));
        } else {
            return Optional.empty();
        }
    }

    @Bean
    public Call.Factory httpClient(HttpTracing httpTracing) {
        return TracingCallFactory.create(httpTracing, new OkHttpClient());
    }

    @Bean
    public HttpClient apacheClient(HttpTracing httpTracing) {
        return new TracedHttpClient(TracingHttpClientBuilder.create(httpTracing).build(), AWSXRay.getGlobalRecorder());
    }

    @Bean
    public StatefulRedisConnection<String, String> fooCache(Tracing tracing) {
        String redisEndpoint = System.getenv("REDIS_ENDPOINT");
        if (redisEndpoint == null) {
            redisEndpoint = "localhost:6379";
        }
        return RedisClient.create(ClientResources.builder()
                                                 .tracing(BraveTracing.create(tracing))
                                                 .build(),
                                  "redis://" + redisEndpoint)
                          .connect();
    }

    public static void main(String[] args) throws Exception {
        if (ENABLE_XRAY_SDK) {
            // Can't use X-Ray propagation with OpenTelemetry if X-Ray SDK is enabled or they would collide.
            OpenTelemetry.setPropagators(DefaultContextPropagators.builder().addHttpTextFormat(new HttpTraceContext()).build());
        } else {
            AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        }
        SpringApplication.run(Application.class, args);
    }
}
