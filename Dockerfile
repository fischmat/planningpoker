FROM amazoncorretto:17.0.10-al2023-headless@sha256:2848d9855d52f7b169bd37f568298ef1c9a04927e749937137c5497735bb2ce2 AS build
WORKDIR /build
COPY . .
RUN yum install findutils -y
RUN scripts/docker/docker-build.sh

FROM amazoncorretto:17.0.10-al2023-headles@sha256:2848d9855d52f7b169bd37f568298ef1c9a04927e749937137c5497735bb2ce2
MAINTAINER github@matthias-fisch.de

RUN mkdir /app
COPY --from=build /build/app.jar /app/planningpoker.jar
WORKDIR /app
RUN chown -R 1000:1000 /app
USER 1000
CMD "java" "-jar" "planningpoker.jar"