FROM openjdk:17-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY target/engine-service-exec.jar engine-service-exec.jar
ENTRYPOINT [\
    "java", \
    "-Xms256m", "-Xmx768m", \
    "-XX:+UseG1GC", "-XX:InitiatingHeapOccupancyPercent=75", "-XX:MaxGCPauseMillis=200", \
    "-jar","/engine-service-exec.jar"\
    ]
