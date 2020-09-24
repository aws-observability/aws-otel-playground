plugins {
    application
    java
    id("com.google.cloud.tools.jib") version "2.5.0"
}

base {
    archivesBaseName = "backend-webflux"
}

application {
    mainClassName = "com.softwareaws.xray.examples.webflux.WebfluxApplication"
}

dependencies {
    implementation("io.lettuce:lettuce-core")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}

jib {
    to {
        image = "ghcr.io/anuraaga/otel-playground-backend-webflux"
    }
    from {
        image = "ghcr.io/anuraaga/aws-opentelemetry-java-base:alpha"
    }
    container {
        environment = mapOf("OTEL_RESOURCE_ATTRIBUTE" to "service.name=WebfluxBackend")
    }
}
