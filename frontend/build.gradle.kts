plugins {
    application
    java
    id("com.google.cloud.tools.jib")
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
        image = "public.ecr.aws/o2z1k4j2/aws-otel-playground:frontend"
    }
    from {
        image = "ghcr.io/anuraaga/aws-opentelemetry-java-base:alpha"
    }
    container {
        environment = mapOf(
                "OTEL_RESOURCE_ATTRIBUTE" to "service.name=OTTest",
                "OTEL_ENDPOINT_PEER_SERVICE_MAPPING" to "tvyfrruhxh.execute-api.us-east-1.amazonaws.com=hello-lambda-api," +
                        "ecs-backend-2093777359.us-east-1.elb.amazonaws.com=ecs-backend," +
                        "2ccd810c-fargate-backend-8661-784022251.us-east-1.elb.amazonaws.com=eks-backend")
    }
}
