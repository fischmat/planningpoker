FROM amazoncorretto:17.0.8-al2023-headless@sha256:03e96648c6439d6e91a65c919d241645cbfea0c677bc716a1dcb628a90a7b745 AS build
WORKDIR /build
COPY . .
RUN yum install findutils -y
RUN scripts/docker/docker-build.sh

FROM amazoncorretto:17.0.8-al2023-headless@sha256:03e96648c6439d6e91a65c919d241645cbfea0c677bc716a1dcb628a90a7b745
MAINTAINER github@matthias-fisch.de

RUN mkdir /app
COPY --from=build /build/app.jar /app/planningpoker.jar
WORKDIR /app
RUN chown -R 1000:1000 /app
USER 1000
CMD "java" "-jar" "planningpoker.jar"