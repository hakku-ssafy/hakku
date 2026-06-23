# --- Build stage ----------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# Gradle wrapper & build scripts first (cache layer)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

# Download dependencies (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build
COPY src/ src/

RUN ./gradlew bootJar -x test --no-daemon

# --- Runtime stage --------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /workspace/build/libs/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
