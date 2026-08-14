FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodbridge && adduser -S foodbridge -G foodbridge
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway.jar app.jar
USER foodbridge
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
