FROM eclipse-temurin:24-jdk-alpine-3.22 AS builder
WORKDIR /build

COPY gradlew gradle.properties build.gradle ./
COPY gradle/wrapper/ gradle/wrapper/

RUN chmod +x gradlew

RUN ./gradlew --no-daemon dependencies

COPY src/ src/

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
