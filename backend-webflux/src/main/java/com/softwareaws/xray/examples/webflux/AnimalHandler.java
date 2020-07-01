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

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AnimalHandler {

    private final RedisReactiveCommands<String, String> catsCache;
    private final RedisReactiveCommands<String, String> dogsCache;

    @Autowired
    public AnimalHandler(
        StatefulRedisConnection<String, String> catsCache,
        StatefulRedisConnection<String, String> dogsCache) {
        this.catsCache = catsCache.reactive();
        this.dogsCache = dogsCache.reactive();
    }

    public Mono<ServerResponse> handle(ServerRequest request) {
        return Flux.merge(
            catsCache.mget("felix", "hobbes"),
            dogsCache.mget("underdog"))
                   .flatMap(results ->
                                Flux.merge(catsCache.mset(Map.of("felix", "the cat",
                                                                 "hobbes", "with calvin",
                                                                 "underdog", "underdog underdog")))
                   )
                   .collectList()
                   .flatMap(result -> ServerResponse.ok()
                                                    .contentType(MediaType.TEXT_PLAIN)
                                                    .body(BodyInserters.fromValue("Enjoy!")));
    }
}
