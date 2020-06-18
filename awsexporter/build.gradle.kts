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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    compileOnly("io.opentelemetry:opentelemetry-sdk-contrib-auto-config")

    implementation("io.opentelemetry:opentelemetry-exporters-otlp") {
        exclude("io.opentelemetry", "opentelemetry-sdk")
    }
    implementation("io.grpc:grpc-api")
    implementation("io.grpc:grpc-netty-shaded")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
    }
}

tasks.jar {
    enabled = false
}
