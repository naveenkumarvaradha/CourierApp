FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/party-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
