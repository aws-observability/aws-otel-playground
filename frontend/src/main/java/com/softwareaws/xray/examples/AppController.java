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
import com.softwareaws.xray.examples.appdb.tables.pojos.Planet;
import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import com.softwareaws.xray.examples.hello.HelloServiceOuterClass;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final DynamoDbClient dynamoDb;
    private final HelloServiceGrpc.HelloServiceBlockingStub helloService;
    private final HttpClient httpClient;
    private final DSLContext appdb;
    private final Tracer tracer;

    @Autowired
    public AppController(DynamoDbClient dynamoDb,
                         HelloServiceGrpc.HelloServiceBlockingStub helloService,
                         HttpClient httpClient,
                         DSLContext appdb,
                         Tracer tracer) {
        this.dynamoDb = dynamoDb;
        this.helloService = helloService;
        this.httpClient = httpClient;
        this.appdb = appdb;
        this.tracer = tracer;
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

        HelloServiceOuterClass.HelloResponse response = helloService.hello(HelloServiceOuterClass.HelloRequest.newBuilder()
                                                                                                              .setName("X-Ray")
                                                                                                              .build());

        var planets = appdb.selectFrom(PLANET)
             .fetchInto(Planet.class);
        String randomPlanet = planets.get(ThreadLocalRandom.current().nextInt(planets.size())).getName();

        final String selfResponseContent;
        try {
            HttpResponse selfResponse = httpClient.execute(new HttpGet("http://localhost:8080/self"));
            selfResponseContent = new String(selfResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch from self.", e);
        }

        return "<html><body>"
               + selfResponseContent + "<br>" + response.getGreeting() + "<br>" + randomPlanet + "<br>" + "Find the traces:<br>"
               + "<a target=\"_blank\" href=\"https://" + AWS_REGION + ".console.aws.amazon.com/xray/home?region=" + AWS_REGION + "#/traces?timeRange=PT1M\">XRay</a><br>"
               + "<a target=\"_blank\" href=\"http://localhost:9412?limit=10&lookback=900000\">zipkin</a><br>"
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
