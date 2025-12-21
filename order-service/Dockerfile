FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
