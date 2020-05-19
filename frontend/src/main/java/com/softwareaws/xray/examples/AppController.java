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

import com.softwareaws.xray.examples.hello.HelloServiceGrpc;
import com.softwareaws.xray.examples.hello.HelloServiceOuterClass;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Controller
public class AppController {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final DynamoDbClient dynamoDb;
    private final HelloServiceGrpc.HelloServiceBlockingStub helloService;
    private final HttpClient httpClient;

    @Autowired
    public AppController(DynamoDbClient dynamoDb,
                         HelloServiceGrpc.HelloServiceBlockingStub helloService,
                         HttpClient httpClient) {
        this.dynamoDb = dynamoDb;
        this.helloService = helloService;
        this.httpClient = httpClient;
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



        final String selfResponseContent;
        try {
            HttpResponse selfResponse = httpClient.execute(new HttpGet("http://localhost:8080/self"));
            selfResponseContent = new String(selfResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch from self.", e);
        }

        return selfResponseContent + "\n" + response.getGreeting();
    }

    @GetMapping("/self")
    @ResponseBody
    public String self() {
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

        return "Read back " + count1 + "," + count2;
    }
}
