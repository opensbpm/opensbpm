FROM openjdk:17-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY target/vaadinui-exec.jar vaadinui-exec.jar
ENTRYPOINT ["java","-jar","/vaadinui-exec.jar"]
