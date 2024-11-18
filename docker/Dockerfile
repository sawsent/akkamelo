FROM openjdk:17

WORKDIR /app

COPY target/akkamelo-api-1.0-SNAPSHOT-allinone.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
