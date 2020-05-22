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

import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TraceProvider;

public class AwsTraceProvider implements TraceProvider {
    @Override
    public TracerProvider create() {
        System.out.println("AwsTraceProvider");
        TracerSdkProvider provider =  TracerSdkProvider.builder()
                                                       .setIdsGenerator(AwsXrayIdsGenerator.INSTANCE)
                                                       .build();
        return provider;
    }
}
