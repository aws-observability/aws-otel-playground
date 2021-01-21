pluginManagement {
    plugins {
        id("com.github.ben-manes.versions") version "0.36.0"
        id("com.github.jk1.dependency-license-report") version "1.16"
        id("com.github.johnrengelman.shadow") version "6.1.0"
        id("com.google.cloud.tools.jib") version "2.7.1"
        id("com.google.protobuf") version "0.8.14"
        id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.6"
        id("de.undercouch.download") version "4.1.1"
    }
}

include(":api")
include(":appdb")
include(":backend")
include(":backend-webflux")
include(":dependencyManagement")
include(":frontend")
include(":lambda-api")
