# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build

# Copy Maven wrapper + pom first (layer cache for dependencies)
COPY mvnw mvnw.cmd* ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy built JAR
COPY --from=build /build/target/courier-booking-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
