# Stage 1: Build the JAR using Java 21 & Maven inside Docker
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java 21 runtime container
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]