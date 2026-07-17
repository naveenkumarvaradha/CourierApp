# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom first (layer cache for dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Copy built JAR
COPY --from=build /build/target/courier-booking-*.jar app.jar

# Writable log directory for the logback FILE appender (prod profile)
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
