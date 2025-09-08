FROM eclipse-temurin:24-jdk-alpine-3.22 AS builder

ARG NEXUS_URL
ARG NEXUS_USER
ARG NEXUS_USER_PASSWORD

ENV NEXUS_URL=http://host.docker.internal:7777/repository/maven-releases/
ENV NEXUS_USER=${NEXUS_USER}
ENV NEXUS_USER_PASSWORD=${NEXUS_USER_PASSWORD}

WORKDIR /build

COPY gradlew gradle.properties build.gradle ./
COPY gradle/wrapper/ gradle/wrapper/

RUN chmod +x gradlew

RUN ./gradlew --no-daemon dependencies

COPY src/ src/
