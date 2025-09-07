FROM eclipse-temurin:24-jdk-alpine-3.22 AS builder

ARG NEXUS_URL
ARG NEXUS_USER
ARG NEXUS_USER_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USER=${NEXUS_USER}
ENV NEXUS_USER_PASSWORD=${NEXUS_USER_PASSWORD}

WORKDIR /build

COPY gradlew gradle.properties build.gradle ./
COPY gradle/wrapper/ gradle/wrapper/

RUN chmod +x gradlew && ./gradlew --no-daemon --refresh-dependencies dependencies

COPY src/ src/

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
