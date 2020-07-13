FROM docker-base-build AS build

ADD . /src
RUN ./gradlew :backend:build

RUN cd /dist && tar --strip-components 1 -xf /src/backend/build/distributions/backend.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTEL_OTLP_ENDPOINT localhost:9100
ENV OTEL_RESOURCE_ATTRIBUTES 'service.name=HelloService'
ENV JAVA_OPTS -javaagent:/app/otel/opentelemetry-auto-aws.jar

CMD /app/bin/backend
