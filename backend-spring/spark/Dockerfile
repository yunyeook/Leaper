FROM openjdk:11-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]