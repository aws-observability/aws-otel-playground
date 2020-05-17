FROM azul/zulu-openjdk-debian:14 AS build

WORKDIR /src

RUN apt-get -y update && apt-get -y install curl

# Copy in minimum files to allow Docker to cache dependencies independently of source changes.
ADD *gradle* /src/
ADD gradle/ /src/gradle/
RUN ./gradlew dependencies

RUN mkdir -p /dist/otel

RUN curl -Lo /dist/otel/opentelemetry-auto.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.2.2/opentelemetry-auto-0.2.2.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-logging.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.2.2/opentelemetry-auto-exporters-logging-0.2.2.jar
RUN curl -Lo /dist/otel/opentelemetry-auto-exporters-otlp.jar https://github.com/open-telemetry/opentelemetry-auto-instr-java/releases/download/v0.2.2/opentelemetry-auto-exporters-otlp-0.2.2.jar

ADD . /src
RUN ./gradlew build

RUN cd /dist && tar --strip-components 1 -xf /src/build/distributions/opentelemetry-collector-xray.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTLP_ENDPOINT localhost:9100

CMD JAVA_OPTS="-javaagent:/app/otel/opentelemetry-auto.jar -Dota.exporter.jar=/app/otel/opentelemetry-auto-exporters-otlp.jar -Dota.exporter.otlp.endpoint=\$OTLP_ENDPOINT" /app/bin/opentelemetry-collector-xray
