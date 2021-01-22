package com.softwareaws.xray.examples.webflux;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.tracing.Tracer.Span;
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
        request.headers().asHttpHeaders().forEach((key, value) -> System.out.println(key + "=" + value));
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
