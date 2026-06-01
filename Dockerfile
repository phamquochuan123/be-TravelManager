# Stage 1: Build dùng Gradle Wrapper (tự tải Gradle 9.2.1)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper trước để cache layer
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Cache dependencies
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source và build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime (chỉ cần JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
