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

import io.opentelemetry.sdk.trace.IdsGenerator;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public enum AwsXrayIdsGenerator implements IdsGenerator {
    INSTANCE;

    private static final long INVALID_ID = 0;

    // Random IDs generation copied from here since not publicly accessible.
    // https://github.com/open-telemetry/opentelemetry-java/blob/master/sdk/src/main/java/io/opentelemetry/sdk/trace/RandomIdsGenerator.java

    @Override
    public SpanId generateSpanId() {
        long id;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        do {
            id = random.nextLong();
        } while (id == INVALID_ID);
        return new SpanId(id);
    }

    @Override
    public TraceId generateTraceId() {
        // Simple implementation that swaps out initially random characters with timestamp, this can be made more efficient with
        // better math (ideally we generate just three numbers, timestamp and two randoms, and concatenate them using math).
        long idHi;
        long idLo;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        do {
            idHi = random.nextLong();
            idLo = random.nextLong();
        } while (idHi == INVALID_ID && idLo == INVALID_ID);
        TraceId randomId =  new TraceId(idHi, idLo);

        String timestamp = Long.toHexString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        String xrayIdString = timestamp + randomId.toLowerBase16().substring(timestamp.length());
        return TraceId.fromLowerBase16(xrayIdString, 0);
    }
}
