# Multi-stage build
FROM openjdk:17-jdk-slim AS builder

# Install required packages
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle wrapper configuration first
COPY gradle gradle
COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY settings.gradle .
COPY build.gradle .

# Download gradle wrapper if missing
RUN if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then \
        echo "Downloading Gradle Wrapper..." && \
        curl -L https://services.gradle.org/distributions/gradle-8.5-bin.zip -o gradle.zip && \
        unzip gradle.zip && \
        cp gradle-8.5/lib/gradle-wrapper.jar gradle/wrapper/ && \
        rm -rf gradle-8.5 gradle.zip; \
    fi

# Make gradlew executable
RUN chmod +x gradlew

# Verify gradle wrapper
RUN ls -la gradle/wrapper/ && ./gradlew --version

# Copy source code
COPY src src

# Build the application (skip tests for faster build)
RUN ./gradlew clean build -x test --no-daemon

# Production stage
FROM openjdk:17-jre-slim

# Create app user
RUN addgroup --system appgroup && adduser --system --group appuser

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder /app/build/libs/*-boot.jar app.jar

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]