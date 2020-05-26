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

import com.rohanprabhu.gradle.plugins.kdjooq.*

plugins {
    `java-library`
    id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.5"
}

dependencies {
    api("org.jooq:jooq:3.12.3")

    jooqGeneratorRuntime("org.mariadb.jdbc:mariadb-java-client:2.6.0")
}

jooqGenerator {
    attachToCompileJava = false

    configuration("appdb", project.sourceSets.main.get()) {
        configuration = jooqCodegenConfiguration {
            jdbc {
                username = "root"
                password = "root"
                driver   = "org.mariadb.jdbc.Driver"
                url      = "jdbc:mysql://localhost:3306/appdb"
            }

            generator {
                database {
                    inputSchema = "appdb"
                }

                target {
                    packageName = "com.softwareaws.xray.examples.appdb"
                    directory   = "${projectDir}/gen-src/"
                }

                generate {
                    isDaos = true
                    isDeprecated = false
                    isFluentSetters = true
                    isImmutableInterfaces = true
                    isImmutablePojos = true
                    isJavaTimeTypes = true
                    isPojosEqualsAndHashCode = true
                    isRecords = true
                    isRelations = true
                }
            }
        }
    }
}

afterEvaluate {
    tasks {
        named("cleanJooq-codegen-appdb") {
            enabled = false
        }
    }
}
