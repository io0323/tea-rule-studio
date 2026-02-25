# Multi-stage build for Tea Rule Studio
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle.kts gradle.properties* ./
COPY gradle ./gradle

# Build the application
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the application
CMD ["java", "-jar", "app.jar"]
