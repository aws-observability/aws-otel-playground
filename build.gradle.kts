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

application {
    mainClassName = "com.softwareaws.xray.examples.Application"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(platform("com.amazonaws:aws-xray-recorder-sdk-bom:2.5.0"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.2.7.RELEASE"))
    implementation(platform("software.amazon.awssdk:bom:2.13.17"))

    implementation("com.amazonaws:aws-xray-recorder-sdk-core")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("software.amazon.awssdk:dynamodb")

    runtimeOnly("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2-instrumentor")
}
