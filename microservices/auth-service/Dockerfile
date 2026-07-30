FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/auth-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
