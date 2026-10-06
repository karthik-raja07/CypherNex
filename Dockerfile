# Stage 1: Build the unified Spring Boot JAR
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy dependency descriptors first for caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add unprivileged user for security
RUN addgroup -S cyphernex && adduser -S cyphernex -G cyphernex
USER cyphernex

# Copy built JAR from builder
COPY --from=builder /app/target/cyphernex-1.0.0-SNAPSHOT.jar app.jar

# Expose server port
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
