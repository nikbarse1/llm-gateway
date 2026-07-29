# Multi-stage build for smaller image size
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Install curl for health checks during build
RUN apk add --no-cache curl

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install required runtime dependencies
RUN apk add --no-cache \
    curl \
    wget \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

# Create application directory and logs directory
RUN mkdir -p /app/logs

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Set JVM options for performance and monitoring
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose port
EXPOSE 8080

# Add labels for metadata
LABEL maintainer="LLM Gateway Team" \
      version="2.0.0" \
      description="Advanced LLM Gateway & Token Optimizer" \
      org.opencontainers.image.source="https://github.com/yourusername/demo-for-llm"

# Health check with proper timeout and retry logic
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f -s http://localhost:8080/actuator/health || exit 1

# Run the application with JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
