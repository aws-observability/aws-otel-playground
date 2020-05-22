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

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class AwsSpanExporterFactory implements SpanExporterFactory {
    private static final String OTLP_ENDPOINT = "otlp.endpoint";

    @Override
    public SpanExporter fromConfig(Config config) {
        System.out.println("AwsSpanExporterFactory");
        // Copied from https://github.com/open-telemetry/opentelemetry-auto-instr-java/blob/master/auto-exporters/otlp/src/main/java/io/opentelemetry/auto/exporters/otlp/OtlpSpanExporterFactory.java
        // because not published to Maven.
        final String otlpEndpoint = config.getString(OTLP_ENDPOINT, "");
        if (otlpEndpoint.isEmpty()) {
            throw new IllegalStateException("ota.exporter.otlp.endpoint is required");
        }
        SpanExporter delegate =
            OtlpGrpcSpanExporter.newBuilder()
                                .setChannel(ManagedChannelBuilder.forTarget(otlpEndpoint).usePlaintext().build())
                                .build();
        return new AwsSpanExporter(delegate);
    }
}
