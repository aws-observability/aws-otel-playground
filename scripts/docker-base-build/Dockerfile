FROM azul/zulu-openjdk-debian:14 AS build

WORKDIR /src

RUN apt-get -y update && apt-get -y install curl

# Copy in minimum files to allow Docker to cache dependencies independently of source changes.
ADD *gradle* /src/
ADD gradle /src/gradle/
ADD api/build.gradle.kts /src/api/
ADD appdb/build.gradle.kts /src/appdb/
ADD awsagentprovider/build.gradle.kts /src/awsagentprovider/
ADD backend/build.gradle.kts /src/backend/
ADD backend-webflux/build.gradle.kts /src/backend-webflux/
ADD dependencyManagement/build.gradle.kts /src/dependencyManagement/
ADD frontend/build.gradle.kts /src/frontend/
ADD otelagent/build.gradle.kts /src/otelagent/
RUN ./gradlew dependencies

ADD awsagentprovider /src/awsagentprovider/
ADD otelagent /src/otelagent/

RUN ./gradlew :otelagent:shadowJar

RUN mkdir -p /dist/otel
RUN cp /src/otelagent/build/libs/otelagent.jar /dist/otel/opentelemetry-auto-aws.jar

CMD echo
