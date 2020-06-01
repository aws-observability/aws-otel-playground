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
ADD frontend/build.gradle.kts /src/frontend/
RUN ./gradlew dependencies

RUN mkdir -p /dist/otel

RUN curl -Lo /dist/otel/opentelemetry-auto.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-0.3.0.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-logging.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-exporters-logging-0.3.0.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-otlp.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-exporters-otlp-0.3.0.jar

ADD awsagentprovider /src/awsagentprovider/
ADD awsexporter /src/awsexporter/

RUN ./gradlew :awsagentprovider:shadowJar :awsexporter:shadowJar
RUN cp /src/awsagentprovider/build/libs/awsagentprovider.jar /dist/otel/opentelemetry-auto-awsagentprovider.jar
RUN cp /src/awsexporter/build/libs/awsexporter.jar /dist/otel/opentelemetry-auto-exporters-awsexporter.jar

ADD . /src
RUN ./gradlew :backend:build

RUN cd /dist && tar --strip-components 1 -xf /src/backend/build/distributions/backend.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTLP_ENDPOINT localhost:9100

CMD JAVA_OPTS="-javaagent:/app/otel/opentelemetry-auto.jar \
  -Xbootclasspath/a:/app/otel/opentelemetry-auto-awsagentprovider.jar \
  -Dio.opentelemetry.auto.shaded.io.opentelemetry.trace.spi.TraceProvider=com.softwareaws.xray.opentelemetry.exporters.AwsTraceProvider \
  -Dota.exporter.jar=/app/otel/opentelemetry-auto-exporters-awsexporter.jar \
  -Dota.exporter.otlp.endpoint=\$OTLP_ENDPOINT" /app/bin/backend
