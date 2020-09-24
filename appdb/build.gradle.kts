import com.rohanprabhu.gradle.plugins.kdjooq.*

plugins {
    `java-library`
    id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.5"
}

dependencies {
    api("org.jooq:jooq")

    jooqGeneratorRuntime("org.mariadb.jdbc:mariadb-java-client")
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
