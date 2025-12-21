# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle bootJar -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/moderation-service-*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
