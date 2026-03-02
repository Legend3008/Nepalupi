# ──────────────────────────────────────────────────────────
# Nepal UPI (NPI) Payment Switch — Multi-stage Dockerfile
# Section 13: Container orchestration
# ──────────────────────────────────────────────────────────

# ── Stage 1: Build ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application (skip tests for Docker build)
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ───────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: run as non-root
RUN groupadd -r npi && useradd -r -g npi -m -d /home/npi npi

WORKDIR /app

# Install dumb-init for proper signal handling (PID 1 problem)
RUN apt-get update && apt-get install -y --no-install-recommends \
    dumb-init curl jq \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy monitoring config
COPY monitoring/ /app/monitoring/

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -sf http://localhost:8081/actuator/health || exit 1

# Switch to non-root user
USER npi

# Expose application port + management port
EXPOSE 8081
EXPOSE 8082

ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
