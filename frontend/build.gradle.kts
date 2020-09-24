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

plugins {
    application
    java
    id("com.google.cloud.tools.jib") version "2.5.0"
}

base {
    archivesBaseName = "frontend"
}

application {
    mainClassName = "com.softwareaws.xray.examples.Application"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":appdb"))

    compileOnly("io.opentelemetry:opentelemetry-exporters-otlp")

    implementation("com.amazonaws:aws-xray-recorder-sdk-apache-http")
    implementation("com.amazonaws:aws-xray-recorder-sdk-core")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.lettuce:lettuce-core")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("mysql:mysql-connector-java")
    implementation("org.apache.httpcomponents:httpclient")
    implementation("org.apache.httpcomponents:httpasyncclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("software.amazon.awssdk:apache-client")
    implementation("software.amazon.awssdk:dynamodb")

    runtimeOnly("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2-instrumentor")
}

jib {
    to {
        image = "ghcr.io/anuraaga/otel-playground-frontend"
    }
    from {
        image = "ghcr.io/anuraaga/aws-opentelemetry-java-base:alpha"
    }
    container {
        environment = mapOf(
                "OTEL_RESOURCE_ATTRIBUTE" to "service.name=OTTest",
                "OTEL_ENDPOINT_PEER_SERVICE_MAPPING" to "tvyfrruhxh.execute-api.us-east-1.amazonaws.com=hello-lambda-api")
    }
}
