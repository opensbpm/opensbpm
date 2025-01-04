FROM openjdk:17-jdk-alpine

WORKDIR /workspace

ARG JAR_FILE=target/*.jar
COPY target/e2e-exec.jar /workspace/e2e-exec.jar
ENTRYPOINT [\
    "java", \
    "-Xms256m", "-Xmx768m", \
    "-XX:+UseG1GC", "-XX:InitiatingHeapOccupancyPercent=75", "-XX:MaxGCPauseMillis=200", \
    "-jar","/workspace/e2e-exec.jar"\
    ]
