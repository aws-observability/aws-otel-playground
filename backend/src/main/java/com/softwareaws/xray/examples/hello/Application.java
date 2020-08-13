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

package com.softwareaws.xray.examples.hello;

import brave.Tracing;
import brave.grpc.GrpcTracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.B3Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;
import zipkin2.reporter.xray_udp.XRayUDPReporter;

public class Application {

    private static final Logger logger = LogManager.getLogger();

    private static void setupSpark() {
        Spark.port(8083);
        Spark.get("/hellospark", (req, res) -> "ok");
    }

    public static void main(String[] args) throws Exception {
        String zipkinEndpoint = System.getenv("ZIPKIN_ENDPOINT");
        if (zipkinEndpoint == null) {
            zipkinEndpoint = "localhost:9411";
        }
        var tracing =
            Tracing.newBuilder()
                   .sampler(Sampler.ALWAYS_SAMPLE)
                   .traceId128Bit(true)
                   .supportsJoin(false)
                   .localServiceName("HelloService")
                   // While Zipkin supports X-Ray propagation, we make sure to use B3 or else remote calls will end up being
                   // merged with the normal X-Ray SDK tracer, which is generally good but not when we want to compare the
                   // tracers to each other.
                   .propagationFactory(B3Propagation.FACTORY)
                   .addSpanHandler(new SpanHandler() {
                       @Override
                       public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                           span.tag("tracer", "zipkin");
                           return true;
                       }
                   })
                   .addSpanHandler(ZipkinSpanHandler.create(XRayUDPReporter.create()))
                   .addSpanHandler(new SpanHandler() {
                       @Override
                       public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                           // Need to make sure trace ID is different so traces show up separately on console. Hacky, but works
                           // for what we need.
                           span.traceId(span.traceId().substring(0, 31) + (span.traceId().endsWith("0") ? "1" : "0"));
                           span.tag("tracer", "zipkin-otel");
                           return true;
                       }
                   })
                   .addSpanHandler(AsyncZipkinSpanHandler.create(OkHttpSender.create("http://" + zipkinEndpoint)))
                   .build();
        var grpcTracing = GrpcTracing.create(tracing);

        var server = ServerBuilder.forPort(8081)
                                  .addService(new HelloService())
                                  .intercept(grpcTracing.newServerInterceptor())
                                  .build();

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdownNow();
            Spark.stop();
        }));

        logger.info("Server started at port 8081.");

        setupSpark();

        server.awaitTermination();
    }
}
