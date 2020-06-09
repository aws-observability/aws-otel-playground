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

import com.softwareaws.xray.opentelemetry.exporters.resources.ResourcePopulator;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceConstants;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TracerProviderFactory;
import java.util.LinkedHashMap;
import java.util.Map;

public class AwsTraceProvider implements TracerProviderFactory {

    private static final String LOCAL_SERVICE_NAME = System.getProperty("ota.aws.service.name", "XrayInstrumentedService");

    @Override
    public TracerProvider create() {
        System.out.println("AwsTraceProvider");

        Map<String, AttributeValue> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put(ResourceConstants.SERVICE_NAME, stringAttributeValue(LOCAL_SERVICE_NAME));

        for (ResourcePopulator populator : ResourcePopulator.getAll()) {
            populator.populate(resourceAttributes);
        }

        return TracerSdkProvider.builder()
                                .setIdsGenerator(AwsXrayIdsGenerator.INSTANCE)
                                .setResource(Resource.create(resourceAttributes))
                                .build();
    }
}
