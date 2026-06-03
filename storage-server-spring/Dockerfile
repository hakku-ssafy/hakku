FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew test bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
RUN mkdir -p /data/images && chown spring:spring /data/images
USER spring
EXPOSE 8082
ENV STORAGE_PORT=8082
ENTRYPOINT ["java", "-jar", "app.jar"]
