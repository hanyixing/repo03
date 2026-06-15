# syntax=docker/dockerfile:1
# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3.6.3-jdk-8-slim AS build
WORKDIR /build

# Resolve dependencies first (better layer caching when only source changes)
COPY pom.xml .
RUN mvn -B -Pprod -DskipTests dependency:go-offline || true

# Build the executable jar with the production profile baked in
COPY src ./src
RUN mvn -B -Pprod -DskipTests clean package

# ---- Runtime stage: slim JRE that only carries the jar ----
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
