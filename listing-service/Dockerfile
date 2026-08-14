FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -pl listing-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodbridge && adduser -S foodbridge -G foodbridge
WORKDIR /app
COPY --from=build /workspace/listing-service/target/listing-service.jar app.jar
USER foodbridge
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
