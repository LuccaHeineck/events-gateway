# === Build stage ===
FROM eclipse-temurin:21-jdk-jammy AS builder
RUN useradd -m gradle
WORKDIR /home/gradle/project

# Copy project files
COPY --chown=gradle:gradle . .

# Normalize line endings and make gradlew executable
RUN sed -i 's/\r$//' ./gradlew || true \
    && chmod +x ./gradlew \
    && ./gradlew clean build -x test --no-daemon

# Copy jar with a fixed name
RUN set -eux; \
    JAR=$(ls build/libs/*.jar | head -n 1); \
    test -n "$JAR"; \
    cp "$JAR" app.jar

# === Runtime stage ===
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /home/gradle/project/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
