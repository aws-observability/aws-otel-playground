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
    java
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        mavenLocal()
    }

    plugins.withId("java") {
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        dependencies {
            implementation(platform("com.amazonaws:aws-xray-recorder-sdk-bom:2.5.0"))
            implementation(platform("io.grpc:grpc-bom:1.29.0"))
            implementation(platform("io.zipkin.brave:brave-bom:5.12.3"))
            implementation(platform("io.zipkin.reporter2:zipkin-reporter-bom:2.15.0"))
            implementation(platform("org.apache.logging.log4j:log4j-bom:2.13.3"))
            implementation(platform("org.springframework.boot:spring-boot-dependencies:2.2.7.RELEASE"))
            implementation(platform("software.amazon.awssdk:bom:2.13.17"))
        }
    }
}
