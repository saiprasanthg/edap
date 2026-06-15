# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache dependencies separately from source code
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build the application (skip tests; tests run in CI separately)
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

LABEL maintainer="edap@example.com"
LABEL description="Engineering Data Access Platform"
LABEL version="1.0.0"

# Non-root user for security
RUN groupadd --system edap && useradd --system --gid edap edap

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Allow the process to bind to privileged ports (needed for gRPC 9090 in some environments)
RUN chown edap:edap app.jar

USER edap

# REST API
EXPOSE 8080
# gRPC
EXPOSE 9090

ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
