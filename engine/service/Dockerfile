FROM openjdk:17-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY target/engine-service-exec.jar engine-service-exec.jar
ENTRYPOINT ["java","-jar","/engine-service-exec.jar"]
