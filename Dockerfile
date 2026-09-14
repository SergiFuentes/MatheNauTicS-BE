# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and build scripts first (better layer caching)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew

# Warm dependency cache
RUN ./gradlew --no-daemon dependencies || true

# Copy sources and build the Spring Boot executable jar.
# Tests run in CI, not during image build.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine AS runtime

# Non-root runtime user
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]