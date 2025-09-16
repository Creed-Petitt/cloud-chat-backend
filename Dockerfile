FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working dir
WORKDIR /app

COPY . .

# Build with Maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]