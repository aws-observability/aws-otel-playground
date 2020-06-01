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

package com.softwareaws.xray.opentelemetry.exporters;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

final class AwsSpanExporter implements SpanExporter {

    private final SpanExporter delegate;

    AwsSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public ResultCode export(Collection<SpanData> spans) {
        List<SpanData> decorated = new ArrayList<>(spans.size());
        for (SpanData span : spans) {
            SpanData decoratedSpan = decorate(span);
            if (decoratedSpan != null) {
                decorated.add(decoratedSpan);
            }
        }
        return delegate.export(Collections.unmodifiableList(decorated));
    }

    @Override
    public ResultCode flush() {
        return delegate.flush();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Nullable
    private static SpanData decorate(SpanData span) {
        String instrumentation = span.getInstrumentationLibraryInfo().getName();
        if (instrumentation.equals("io.opentelemetry.auto.aws-sdk-2.2")) {
            return toBuilder(span).setKind(Span.Kind.CLIENT).build();
        } else if (instrumentation.equals("io.opentelemetry.auto.apache-httpclient-4.0")) {
            // Suppress all Apache HTTP calls for now until we have a story for interaction between RPC + transport.
            // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/440
            return null;
        }
        return span;
    }

    private static SpanData.Builder toBuilder(SpanData span) {
        return SpanData.newBuilder()
                       .setTraceId(span.getTraceId())
                       .setSpanId(span.getSpanId())
                       .setTraceFlags(span.getTraceFlags())
                       .setTraceState(span.getTraceState())
                       .setParentSpanId(span.getParentSpanId())
                       .setInstrumentationLibraryInfo(span.getInstrumentationLibraryInfo())
                       .setName(span.getName())
                       .setStartEpochNanos(span.getStartEpochNanos())
                       .setEndEpochNanos(span.getEndEpochNanos())
                       .setAttributes(span.getAttributes())
                       .setTimedEvents(span.getTimedEvents())
                       .setStatus(span.getStatus())
                       .setKind(span.getKind())
                       .setLinks(span.getLinks())
                       .setHasRemoteParent(span.getHasRemoteParent())
                       .setHasEnded(span.getHasEnded())
                       .setTotalRecordedEvents(span.getTotalRecordedEvents())
                       .setTotalRecordedLinks(span.getTotalRecordedLinks())
                       .setTotalAttributeCount(span.getTotalAttributeCount())
                       .setResource(span.getResource());
    }
}
