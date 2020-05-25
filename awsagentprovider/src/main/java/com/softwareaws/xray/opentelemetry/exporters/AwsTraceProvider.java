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

import com.softwareaws.xray.opentelemetry.exporters.resources.ResourcePopulator;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TraceProvider;
import java.util.LinkedHashMap;
import java.util.Map;

public class AwsTraceProvider implements TraceProvider {
    @Override
    public TracerProvider create() {
        System.out.println("AwsTraceProvider");

        Map<String, AttributeValue> resourceAttributes = new LinkedHashMap<>();
        for (ResourcePopulator populator : ResourcePopulator.getAll()) {
            populator.populate(resourceAttributes);
        }

        TracerSdkProvider.Builder provider =  TracerSdkProvider.builder()
                                                               .setIdsGenerator(AwsXrayIdsGenerator.INSTANCE);
        if (!resourceAttributes.isEmpty()) {
            provider.setResource(Resource.create(resourceAttributes));
        }
        return provider.build();
    }
}
