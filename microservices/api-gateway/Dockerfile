FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
