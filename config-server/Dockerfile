FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=50s \
    CMD wget --quiet --tries=1 --spider http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
