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

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.proxies.apache.http.TracedHttpClient;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import java.net.URI;
import javax.servlet.Filter;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootApplication
public class Application {

    @interface Cats {
    }

    private static final boolean ENABLE_XRAY_SDK = "true".equals(System.getenv("ENABLE_XRAY_SDK"));

    private static class OnXrayEnabled implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return ENABLE_XRAY_SDK;
        }
    }

    @Bean
    public DynamoDbClient dynamoDb() {
        var builder = DynamoDbClient.builder();
        String dynamodbEndpoint = System.getenv("DYNAMODB_ENDPOINT");
        if (dynamodbEndpoint != null) {
            builder.endpointOverride(URI.create("http://" + dynamodbEndpoint));
        }
        return builder.build();
    }

    @Bean
    public HelloServiceGrpc.HelloServiceBlockingStub helloService() {
        String helloServiceEndpoint = System.getenv("HELLO_SERVICE_ENDPOINT");
        if (helloServiceEndpoint == null) {
            helloServiceEndpoint = "localhost:8081";
        }
        return HelloServiceGrpc.newBlockingStub(ManagedChannelBuilder
                                                    .forTarget(helloServiceEndpoint)
                                                    .usePlaintext()
                                                    .build());
    }

    @Bean
    @Conditional(OnXrayEnabled.class)
    public Filter servletFilter() {
        return new AWSXRayServletFilter("OTTest");
    }

    @Bean
    public Call.Factory httpClient() {
        return new OkHttpClient();
    }

    @Bean
    public HttpClient apacheClient() {
        return new TracedHttpClient(HttpClients.createDefault());
    }

    @Bean
    public HttpAsyncClient apacheAsyncClient() {
        var client = HttpAsyncClients.createDefault();
        client.start();
        return client;
    }

    @Bean
    public StatefulRedisConnection<String, String> catsCache() {
        String redisEndpoint = System.getenv("CATS_CACHE_ENDPOINT");
        if (redisEndpoint == null) {
            redisEndpoint = "localhost:6379";
        }
        return RedisClient.create("redis://" + redisEndpoint)
                          .connect();
    }

    @Bean
    public StatefulRedisConnection<String, String> dogsCache() {
        String redisEndpoint = System.getenv("DOGS_CACHE_ENDPOINT");
        if (redisEndpoint == null) {
            redisEndpoint = "localhost:6380";
        }
        return RedisClient.create("redis://" + redisEndpoint)
                          .connect();
    }

    public static void main(String[] args) throws Exception {
        if (ENABLE_XRAY_SDK) {
            // Can't use X-Ray propagation with OpenTelemetry if X-Ray SDK is enabled or they would collide.
            OpenTelemetry.setPropagators(DefaultContextPropagators.builder().addTextMapPropagator(new HttpTraceContext()).build());
        } else {
            AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        }
        SpringApplication.run(Application.class, args);
    }
}
