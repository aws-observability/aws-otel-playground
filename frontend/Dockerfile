FROM docker-base-build AS build

ADD . /src
RUN ./gradlew :frontend:build

RUN cd /dist && tar --strip-components 1 -xf /src/frontend/build/distributions/frontend.tar

FROM amazoncorretto:11

COPY --from=build /dist /app

WORKDIR /app

ENV OTA_EXPORTER_OTLP_ENDPOINT localhost:9100
ENV OTEL_RESOURCE_ATTRIBUTES 'service.name=OTTest'
ENV OTA_ENDPOINT_PEER_SERVICE_MAPPING '4cz4hdh1wb.execute-api.us-west-2.amazonaws.com=lambda-service'
ENV JAVA_OPTS -javaagent:/app/otel/opentelemetry-auto-aws.jar

CMD /app/bin/frontend
