# ---- Build stage: compile the app with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the Maven files first (for better layer caching)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Copy the source and build the jar (skip tests to speed up the build)
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Run stage: run the built jar on a smaller Java image ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/customer-form-hub-0.0.1-SNAPSHOT.jar app.jar

# Render provides the PORT env var; our app already reads it
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]