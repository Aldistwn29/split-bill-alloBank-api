# Multi-stage build template for Allo Bank Backend Challenge
# Candidates must include a Dockerfile in the root of their project.
# You may modify this template, but the final image must:
#   - Build the application using Maven
#   - Run on port 4110
#   - Start without requiring any manual steps

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache for dependencies)
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
RUN chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline


# Copy source and build
COPY src ./src
RUN ./mvnw -B -q package -DskipTests


# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV SERVER_PORT=4110
EXPOSE 4110
ENTRYPOINT ["java", "-jar", "app.jar"]
