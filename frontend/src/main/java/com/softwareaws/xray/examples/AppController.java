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

import static com.softwareaws.xray.examples.appdb.tables.Planet.PLANET;

import brave.Tracer;
import com.google.common.util.concurrent.Futures;
import com.softwareaws.xray.examples.appdb.tables.pojos.Planet;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import com.softwareaws.xray.examples.hello.HelloServiceOuterClass;
import io.lettuce.core.api.StatefulRedisConnection;
import io.opentelemetry.trace.TracingContextUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Controller
public class AppController {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final String AWS_REGION = Objects.requireNonNullElse(System.getenv("AWS_REGION"), "insert-region");

    private static final String WEBFLUX_BACKEND_ENDPOINT = System.getenv().getOrDefault("WEBFLUX_BACKEND_ENDPOINT", "localhost:8082");
    private static final String SPARK_BACKEND_ENDPOINT = System.getenv().getOrDefault("SPARK_BACKEND_ENDPOINT", "localhost:8083");

    private final DynamoDbClient dynamoDb;
    private final HelloServiceGrpc.HelloServiceBlockingStub helloService;
    private final Call.Factory httpClient;
    private final HttpClient apacheClient;
    private final HttpAsyncClient apacheAsyncClient;
    private final DSLContext appdb;
    private final StatefulRedisConnection<String, String> catsCache;
    private final StatefulRedisConnection<String, String> dogsCache;
    private final Tracer tracer;
    private final String serverPort;

    @Autowired
    public AppController(DynamoDbClient dynamoDb,
                         HelloServiceGrpc.HelloServiceBlockingStub helloService,
                         Call.Factory httpClient,
                         HttpClient apacheClient,
                         HttpAsyncClient apacheAsyncClient, DSLContext appdb,
                         StatefulRedisConnection<String, String> catsCache,
                         StatefulRedisConnection<String, String> dogsCache,
                         Tracer tracer,
                         @Value("${server.port}") String serverPort) {
        this.dynamoDb = dynamoDb;
        this.helloService = helloService;
        this.httpClient = httpClient;
        this.apacheClient = apacheClient;
        this.apacheAsyncClient = apacheAsyncClient;
        this.appdb = appdb;
        this.catsCache = catsCache;
        this.dogsCache = dogsCache;
        this.tracer = tracer;
        this.serverPort = serverPort;
    }

    @GetMapping("/")
    @ResponseBody
    public String root() {
        dynamoDb.putItem(
            PutItemRequest.builder()
                          .tableName("scratch")
                          .item(
                              Map.of("id", AttributeValue.builder().s("1").build(),
                                     "count", AttributeValue.builder()
                                                            .n(String.valueOf(COUNTER.getAndIncrement()))
                                                            .build()))
                          .build());
        dynamoDb.putItem(PutItemRequest.builder()
                                       .tableName("scratch")
                                       .item(
                                           Map.of("id", AttributeValue.builder().s("2").build(),
                                                  "count", AttributeValue.builder()
                                                                         .n(String.valueOf(COUNTER.getAndIncrement()))
                                                                         .build()))
                                       .build());

        catsCache.sync().get("garfield");
        dogsCache.sync().get("odie");

        HelloServiceOuterClass.HelloResponse response = helloService.hello(HelloServiceOuterClass.HelloRequest.newBuilder()
                                                                                                              .setName("X-Ray")
                                                                                                              .build());

        try {
            helloService.fail(HelloServiceOuterClass.FailRequest.newBuilder().setReason("bad cat").build());
        } catch (Throwable t) {
            // Ignore. It should be in our trace though!
        }
        try {
            helloService.badRequest(HelloServiceOuterClass.FailRequest.newBuilder().setReason("bad to the bone").build());
        } catch (Throwable t) {
            // Ignore. It should be in our trace though!
        }
        try {
            helloService.throttled(HelloServiceOuterClass.FailRequest.newBuilder().setReason("too fast").build());
        } catch (Throwable t) {
            // Ignore. It should be in our trace though!
        }

        try {
            apacheClient.execute(new HttpGet("https://4cz4hdh1wb.execute-api.us-west-2.amazonaws.com/ot-test/"))
                        .getEntity().getContent().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch from lambda API", e);
        }

        Futures.getUnchecked(
            apacheAsyncClient.execute(new HttpGet("http://" + WEBFLUX_BACKEND_ENDPOINT + "/"),
                                      new FutureCallback<>() {
                                          @Override
                                          public void completed(HttpResponse result) {
                                          }

                                          @Override
                                          public void failed(Exception ex) {
                                          }

                                          @Override
                                          public void cancelled() {
                                          }
                                      }));

        var planets = appdb.selectFrom(PLANET)
                           .fetchInto(Planet.class);
        String randomPlanet = planets.get(ThreadLocalRandom.current().nextInt(planets.size())).getName();

        final String selfResponseContent;
        try (Response selfResponse = httpClient.newCall(
            new Request.Builder().url("http://localhost:" + serverPort + "/self").build()).execute()) {
            selfResponseContent = selfResponse.body().string();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch from self.", e);
        }

        try (Response sparkResponse =
                 httpClient.newCall(new Request.Builder().url("http://" + SPARK_BACKEND_ENDPOINT + "/hellospark").build()).execute()) {
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch from spark.", e);
        }

        catsCache.sync().set("garfield", "fat");
        dogsCache.sync().set("odie", "funny");

        String traceId = TracingContextUtils.getCurrentSpan().getContext().getTraceId().toLowerBase16();
        return "<html><body>"
               + selfResponseContent + "<br>" + response.getGreeting() + "<br>" + randomPlanet + "<br>" + "Find the traces:<br>"
               + "<a target=\"_blank\" href=\"https://" + AWS_REGION + ".console.aws.amazon.com/xray/home?region=" + AWS_REGION + "#/traces?timeRange=PT1M\">XRay</a>"
               + "<a target=\"_blank\" href=\"" + xrayUrl(traceId) + "\">[Trace]</a><br>"
               + "<a target=\"_blank\" href=\"http://localhost:9412?limit=10&lookback=900000\">zipkin</a>"
               + "<a target=\"_blank\" href=\"" + zipkinUrl(traceId) + "\">[Trace]</a><br>"
               + "<a target=\"_blank\" href=\"http://localhost:16686?limit=20&lookback=1h&maxDuration&minDuration&service=OTTest\">jaeger</a>"
               + "</body></html>";
    }

    @GetMapping("/self")
    @ResponseBody
    public String self(@RequestHeader Map<String, String> headers) {
        String count1 = dynamoDb.getItem(GetItemRequest.builder()
                                                       .tableName("scratch")
                                                       .key(Map.of("id", AttributeValue.builder()
                                                                                       .s("1")
                                                                                       .build()))
                                                       .build())
                                .item()
                                .get("count")
                                .n();

        String count2 = dynamoDb.getItem(GetItemRequest.builder()
                                                       .tableName("scratch")
                                                       .key(Map.of("id", AttributeValue.builder()
                                                                                       .s("2")
                                                                                       .build()))
                                                       .build())
                                .item()
                                .get("count")
                                .n();

        return "Read back " + count1 + "," + count2 + "<br>Got headers:" + headers;
    }

    private static String xrayUrl(String traceId) {
        return "https://" + AWS_REGION + ".console.aws.amazon.com/xray/home?region=" + AWS_REGION + "#/traces/1-" + traceId.substring(0, 8)
               + "-" + traceId.substring(8);
    }

    private static String zipkinUrl(String traceId) {
        return "http://localhost:9412/zipkin/traces/" + traceId;
    }

    private static String jaegerUrl(String traceId) {
        return "http://localhost:9412/zipkin/traces/" + traceId;
    }
}
