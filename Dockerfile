# Build stage using Gradle and Eclipse Temurin Java 21
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Copy gradle files for caching layers
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Grant executable permission and download dependencies
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build production jar
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security best practice
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 --ingroup appgroup appuser
USER appuser

COPY --from=builder /workspace/build/libs/*.jar app.jar

# Render supplies PORT for the web service.
# Keep 8080 as the local/default Spring Boot port.
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar app.jar"]