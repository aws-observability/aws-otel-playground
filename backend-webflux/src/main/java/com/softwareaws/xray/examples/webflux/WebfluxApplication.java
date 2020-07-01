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

package com.softwareaws.xray.examples.webflux;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootApplication
public class WebfluxApplication {

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

    @Bean
    public RouterFunction<ServerResponse> route(AnimalHandler animalHandler) {
        return RouterFunctions
            .route(RequestPredicates.GET("/").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), animalHandler::handle);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebfluxApplication.class, args);
    }
}
