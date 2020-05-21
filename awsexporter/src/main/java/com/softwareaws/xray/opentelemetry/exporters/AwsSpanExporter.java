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

import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceConstants;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AwsSpanExporter implements SpanExporter {

    private static final Resource DUMMY_RESOURCE;

    static {
        Map<String, AttributeValue> labels = new HashMap<>();
        labels.put(ResourceConstants.CLOUD_PROVIDER, AttributeValue.stringAttributeValue("aws"));
        labels.put(ResourceConstants.HOST_ID, AttributeValue.stringAttributeValue("instance-12345"));
        labels.put(ResourceConstants.CLOUD_ZONE, AttributeValue.stringAttributeValue("ap-northeast1-a"));
        DUMMY_RESOURCE = Resource.create(labels);
    }

    private final SpanExporter delegate;

    AwsSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public ResultCode export(Collection<SpanData> spans) {
        List<SpanData> decorated = new ArrayList<>(spans.size());
        for (SpanData span : spans) {
            decorated.add(decorate(span));
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

    private static SpanData decorate(SpanData span) {
        if (!span.getResource().equals(Resource.getEmpty())) {
            return span;
        }
        // TODO(anuraaga): If this ends up as the official method for customizing spans, really need to add SpanData.toBuilder
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
                       .setResource(DUMMY_RESOURCE)
                       .build();
    }
}
