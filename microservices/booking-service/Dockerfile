FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/booking-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
