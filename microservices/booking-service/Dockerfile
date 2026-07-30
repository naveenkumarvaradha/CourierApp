FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/booking-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
