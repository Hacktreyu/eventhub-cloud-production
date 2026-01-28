# Build stage - Optimized for Render Free Tier
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy source
COPY pom.xml .
COPY src src

# Build with memory limit (prevents freezing on free tier)
# Skip tests to save time
ENV MAVEN_OPTS="-Xmx350m -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run application with memory limits for free tier (512MB total)
# -Xmx400m: Max heap 400MB (leaves ~100MB for metaspace/threads)
# -XX:+UseSerialGC: Lightweight GC for low memory
# -Dserver.address=0.0.0.0: Bind to all interfaces (required by Render)
ENTRYPOINT ["java", \
  "-Xmx400m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=100m", \
  "-Dserver.address=0.0.0.0", \
  "-jar", \
  "app.jar"]
