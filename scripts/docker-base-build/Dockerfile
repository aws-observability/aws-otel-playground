FROM azul/zulu-openjdk-debian:14 AS build

WORKDIR /src

RUN apt-get -y update && apt-get -y install curl

# Copy in minimum files to allow Docker to cache dependencies independently of source changes.
ADD *gradle* /src/
ADD gradle /src/gradle/
ADD api/build.gradle.kts /src/api/
ADD appdb/build.gradle.kts /src/appdb/
ADD backend/build.gradle.kts /src/backend/
ADD backend-webflux/build.gradle.kts /src/backend-webflux/
ADD dependencyManagement/build.gradle.kts /src/dependencyManagement/
ADD frontend/build.gradle.kts /src/frontend/
RUN ./gradlew dependencies

RUN mkdir -p /dist/otel
ADD https://github.com/anuraaga/aws-opentelemetry-java-instrumentation/releases/download/v0.8.0-alpha.1/aws-opentelemetry-agent.jar /dist/otel/opentelemetry-auto-aws.jar
