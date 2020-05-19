FROM azul/zulu-openjdk-debian:14 AS build

WORKDIR /src

RUN apt-get -y update && apt-get -y install curl

# Copy in minimum files to allow Docker to cache dependencies independently of source changes.
ADD *gradle* /src/
ADD gradle /src/gradle/
ADD api/build.gradle.kts /src/api/
ADD frontend/build.gradle.kts /src/frontend/
RUN ./gradlew dependencies

RUN mkdir -p /dist/otel

RUN curl -Lo /dist/otel/opentelemetry-auto.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-0.3.0.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-logging.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-exporters-logging-0.3.0.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-otlp.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.3.0/opentelemetry-auto-exporters-otlp-0.3.0.jar

ADD . /src
RUN ./gradlew :frontend:build

RUN cd /dist && tar --strip-components 1 -xf /src/frontend/build/distributions/frontend.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTLP_ENDPOINT localhost:9100

CMD JAVA_OPTS="-javaagent:/app/otel/opentelemetry-auto.jar -Dota.exporter.jar=/app/otel/opentelemetry-auto-exporters-otlp.jar -Dota.exporter.otlp.endpoint=\$OTLP_ENDPOINT" /app/bin/frontend
