# Use Maven image for building the application
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the Maven configuration and source files
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package

# Create the runtime image
FROM openjdk:17

# Set the working directory for the runtime
WORKDIR /app

# Copy the compiled JAR from the build stage
COPY --from=build /app/target/akkamelo-api-1.0-SNAPSHOT-allinone.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the entrypoint to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar", "--server.address=0.0.0.0"]
