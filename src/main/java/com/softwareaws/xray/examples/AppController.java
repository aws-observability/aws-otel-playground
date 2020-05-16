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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Autowired
    public AppController(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
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
