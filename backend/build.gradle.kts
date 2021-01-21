plugins {
    application
    java
    id("com.google.cloud.tools.jib")
}

application {
    mainClassName = "com.softwareaws.xray.examples.hello.Application"
}

dependencies {
    implementation(project(":api"))

    implementation("com.sparkjava:spark-core")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("org.apache.logging.log4j:log4j-core")

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl")
    runtimeOnly("io.opentelemetry:opentelemetry-sdk")
}

jib {
    to {
        image = "public.ecr.aws/o2z1k4j2/aws-otel-playground:backend"
    }
    from {
        image = "ghcr.io/anuraaga/aws-opentelemetry-java-base:alpha"
    }
    container {
        environment = mapOf("OTEL_RESOURCE_ATTRIBUTE" to "service.name=HelloService,cloud.provider=onprem")
    }
}
