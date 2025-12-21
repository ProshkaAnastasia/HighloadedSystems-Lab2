FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew bootJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
