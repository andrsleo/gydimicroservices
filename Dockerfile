# Multi-stage Dockerfile for GYDI 2.0 Backend (Spring Boot 3.5.5 + Java 21)
# Optimized for Railway deployment

# ============================================
# Build Stage
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Install Maven (Railway doesn't include it by default)
RUN apk add --no-cache maven

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Verify JAR was created
RUN ls -lh target/*.jar

# ============================================
# Runtime Stage
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Install utilities for health checks
RUN apk --no-cache add curl wget && \
    rm -rf /var/cache/apk/*

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port (Railway will override with $PORT)
EXPOSE 8080

# Health check (Railway uses this to verify the app is running)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# JVM optimization for Railway (512 MB RAM limit)
ENV JAVA_OPTS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run the application
# Railway injects $PORT automatically
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
