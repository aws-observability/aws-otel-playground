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

    implementation("com.amazonaws:aws-xray-recorder-sdk-apache-http")
    implementation("com.amazonaws:aws-xray-recorder-sdk-core")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.zipkin.aws:brave-instrumentation-aws-java-sdk-v2-core:0.21.1")
    implementation("io.zipkin.aws:zipkin-reporter-xray-udp:0.21.1")
    implementation("io.zipkin.brave:brave-instrumentation-grpc")
    implementation("io.zipkin.brave:brave-instrumentation-okhttp3")
    implementation("io.zipkin.brave:brave-instrumentation-spring-webmvc")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("mysql:mysql-connector-java:8.0.20")
    implementation("org.apache.httpcomponents:httpclient:4.5.12")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq") {
        // X-Ray SDK only supports Tomcat connection pool so use it for comparison. OpenTelemetry users should stick
        // to HikariCP for a real app.
        exclude("com.zaxxer", "HikariCP")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.squareup.okhttp3:okhttp:4.7.2")
    implementation("software.amazon.awssdk:apache-client")
    implementation("software.amazon.awssdk:dynamodb")

    runtimeOnly("com.amazonaws:aws-xray-recorder-sdk-sql-mysql")
    runtimeOnly("io.zipkin.brave:brave-instrumentation-mysql8")
    runtimeOnly("org.apache.tomcat:tomcat-jdbc")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:2.6.0")

    runtimeOnly("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2-instrumentor")
    runtimeOnly("io.opentelemetry:opentelemetry-sdk:0.4.1")
}
