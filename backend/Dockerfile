FROM azul/zulu-openjdk-debian:14 AS build

WORKDIR /src

RUN apt-get -y update && apt-get -y install curl

# Copy in minimum files to allow Docker to cache dependencies independently of source changes.
ADD *gradle* /src/
ADD gradle /src/gradle/
ADD api/build.gradle.kts /src/api/
ADD appdb/build.gradle.kts /src/appdb/
ADD awsagentprovider/build.gradle.kts /src/awsagentprovider/
ADD awsexporter/build.gradle.kts /src/awsexporter/
ADD backend/build.gradle.kts /src/backend/
ADD dependencyManagement/build.gradle.kts /src/dependencyManagement/
ADD frontend/build.gradle.kts /src/frontend/
ADD otelagent/build.gradle.kts /src/otelagent/
RUN ./gradlew dependencies

RUN mkdir -p /dist/otel

ADD awsagentprovider /src/awsagentprovider/
ADD otelagent /src/otelagent/

RUN ./gradlew :otelagent:shadowJar

RUN mkdir -p /dist/otel
RUN cp /src/otelagent/build/libs/otelagent.jar /dist/otel/opentelemetry-auto-aws.jar

ADD . /src
RUN ./gradlew :backend:build

RUN cd /dist && tar --strip-components 1 -xf /src/backend/build/distributions/backend.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTLP_ENDPOINT localhost:9100

CMD JAVA_OPTS="-javaagent:/app/otel/opentelemetry-auto-aws.jar \
  -Dota.aws.service.name=OTTestBackend \
  -Dota.exporter.otlp.endpoint=\$OTLP_ENDPOINT" /app/bin/backend
