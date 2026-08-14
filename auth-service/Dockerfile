# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
# Built from the reactor root in docker-compose (build context = repo root); see docker-compose.yml
RUN --mount=type=cache,target=/root/.m2 ./mvnw -pl auth-service -am -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodbridge && adduser -S foodbridge -G foodbridge
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service.jar app.jar
USER foodbridge
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
