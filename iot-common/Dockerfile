FROM eclipse-temurin:24-jdk-alpine-3.22 AS builder
WORKDIR /build

COPY gradlew gradle.properties build.gradle ./
COPY gradle/wrapper/ gradle/wrapper/
COPY src/ src/

RUN chmod +x gradlew
RUN ./gradlew --no-daemon clean dependencies

