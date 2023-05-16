FROM amazoncorretto:17.0.7-al2023-headless@sha256:4b35c22296b8383d76a37776a2f392228a57340c09a608865ef630bcae080c16 AS build
WORKDIR /build
COPY . .
RUN yum install findutils -y
RUN scripts/docker/docker-build.sh

FROM amazoncorretto:17.0.7-al2023-headless@sha256:4b35c22296b8383d76a37776a2f392228a57340c09a608865ef630bcae080c16
MAINTAINER github@matthias-fisch.de

RUN mkdir /app
COPY --from=build /build/app.jar /app/planningpoker.jar
WORKDIR /app
RUN chown -R 1000:1000 /app
USER 1000
CMD "java" "-jar" "planningpoker.jar"