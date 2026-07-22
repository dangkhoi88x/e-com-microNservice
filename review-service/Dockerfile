FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/review-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
