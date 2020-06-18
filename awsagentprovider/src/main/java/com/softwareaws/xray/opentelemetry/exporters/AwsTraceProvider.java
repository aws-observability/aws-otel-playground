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

import static io.opentelemetry.common.AttributeValue.stringAttributeValue;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.sdk.contrib.trace.aws.AwsXRayIdsGenerator;
import io.opentelemetry.sdk.contrib.trace.aws.resource.AwsResource;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceConstants;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TracerProviderFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwsTraceProvider implements TracerProviderFactory {

    private static final String LOCAL_SERVICE_NAME = System.getProperty("ota.aws.service.name", "XrayInstrumentedService");

    private static final Logger logger = LoggerFactory.getLogger(AwsTraceProvider.class);

    private static final TracerSdkProvider TRACER_PROVIDER;
    static {

        Map<String, AttributeValue> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put(ResourceConstants.SERVICE_NAME, stringAttributeValue(LOCAL_SERVICE_NAME));

        for (Map.Entry<String, AttributeValue> entry : AwsResource.create().getAttributes().entrySet()) {
            resourceAttributes.put(entry.getKey(), entry.getValue());
        }

        TRACER_PROVIDER = TracerSdkProvider.builder()
                                           .setIdsGenerator(new AwsXRayIdsGenerator())
                                           .setResource(Resource.create(resourceAttributes))
                                           .build();

        // Hackily ensure the propagators are set after the provider is initialized.
        // TODO(anuraaga): Remove hack https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/387
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                logger.info("Registering AwsXrayPropagator propagator.");
                OpenTelemetry.setPropagators(DefaultContextPropagators.builder()
                                                                      .addHttpTextFormat(new AwsXrayPropagator())
                                                                      .build());
            }
        }).start();
    }

    @Override
    public TracerProvider create() {
        System.out.println("AwsTraceProvider");
        return TRACER_PROVIDER;
    }
}
