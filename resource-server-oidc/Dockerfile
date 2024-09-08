FROM openjdk:17-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY target/resource-server-exec.jar resource-server-exec.jar
ENTRYPOINT ["java","-jar","/resource-server-exec.jar"]
