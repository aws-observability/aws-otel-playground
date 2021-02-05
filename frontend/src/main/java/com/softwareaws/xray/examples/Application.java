package com.softwareaws.xray.examples;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.proxies.apache.http.TracedHttpClient;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import java.io.IOException;
import java.net.URI;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.stereotype.Component;
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

    @Component
    public static class SpanReturningFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
            HttpServletResponse servletResponse = (HttpServletResponse) response;
            GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), servletResponse, HttpServletResponse::setHeader);
            chain.doFilter(request, response);
        }
    }

    public static void main(String[] args) throws Exception {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        SpringApplication.run(Application.class, args);
    }
}
