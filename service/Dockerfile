
#FROM eclipse-temurin:17-jdk-alpine AS build
FROM gradle:8.5-jdk17 AS build
WORKDIR /workspace/app

COPY . .

#RUN ./gradlew bootJar
RUN gradle bootJar
RUN mkdir -p build/distribution && (cd build/distribution; jar -xf ../libs/*.jar)

FROM eclipse-temurin:17-jdk-alpine AS prod
VOLUME /tmp
ARG DISTRIBUTION=/workspace/app/build/distribution
COPY --from=build ${DISTRIBUTION}/BOOT-INF/lib /app/lib
COPY --from=build ${DISTRIBUTION}/META-INF /app/META-INF
COPY --from=build ${DISTRIBUTION}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","org.opensbpm.service.OpenSbpmServiceApplication"]
