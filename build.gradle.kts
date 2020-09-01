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

        maven {
            setUrl("https://oss.jfrog.org/libs-snapshot")
        }
    }

    plugins.withId("java") {
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        dependencies {
            configurations.configureEach {
                add(name, enforcedPlatform(project(":dependencyManagement")))
            }
        }
    }
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "6.6.1"
    distributionType = Wrapper.DistributionType.ALL
    distributionSha256Sum = "11657af6356b7587bfb37287b5992e94a9686d5c8a0a1b60b87b9928a2decde5"
}
