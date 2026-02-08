FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/promotion-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 9010

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
