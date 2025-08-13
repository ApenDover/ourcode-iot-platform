FROM openjdk:24
WORKDIR /opt/event-collector/
ARG CACHEBUST=1
COPY build/libs/app.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
