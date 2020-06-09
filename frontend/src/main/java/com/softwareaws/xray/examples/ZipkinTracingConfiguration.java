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

import brave.Tracer;
import brave.Tracing;
import brave.grpc.GrpcTracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.http.HttpTracing;
import brave.instrumentation.awsv2.AwsSdkTracing;
import brave.propagation.B3Propagation;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import brave.spring.webmvc.DelegatingTracingFilter;
import brave.spring.webmvc.SpanCustomizingAsyncHandlerInterceptor;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;
import zipkin2.reporter.xray_udp.XRayUDPReporter;

@Configuration
@Import(SpanCustomizingAsyncHandlerInterceptor.class)
public class ZipkinTracingConfiguration implements WebMvcConfigurer {

    @Bean
    public SpanHandler xraySpanHandler() {
        return ZipkinSpanHandler.create(XRayUDPReporter.create());
    }

    @Bean
    public SpanHandler otelSpanHandler() {
        String zipkinEndpoint = System.getenv("ZIPKIN_ENDPOINT");
        if (zipkinEndpoint == null) {
            zipkinEndpoint = "localhost:9411";
        }
        return AsyncZipkinSpanHandler.create(OkHttpSender.create("http://" + zipkinEndpoint));
    }

    @Bean
    public Tracing tracing() {
        var tracing =
            Tracing.newBuilder()
                   .sampler(Sampler.ALWAYS_SAMPLE)
                   .traceId128Bit(true)
                   .supportsJoin(false)
                   .localServiceName("OTTest")
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
                   .addSpanHandler(xraySpanHandler())
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
                   .addSpanHandler(otelSpanHandler());
        return tracing.build();
    }

    @Bean
    public Tracer tracer(Tracing tracing) {
        return tracing.tracer();
    }

    @Bean
    public HttpTracing httpTracing(Tracing tracing) {
        return HttpTracing.create(tracing);
    }

    @Bean
    public GrpcTracing grpcTracing(Tracing tracing) {
        return GrpcTracing.create(tracing);
    }

    @Bean
    public Filter zipkinFilter() {
        return new DelegatingTracingFilter();
    }

    @Bean
    public AwsSdkTracing awsSdkTracing(HttpTracing httpTracing) {
        return AwsSdkTracing.create(httpTracing);
    }

    @Autowired
    SpanCustomizingAsyncHandlerInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
