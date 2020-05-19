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

import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.12"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.12.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.29.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-protobuf")
    compileOnly("jakarta.annotation:jakarta.annotation-api:1.3.5")
}
