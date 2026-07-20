FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/promotion-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
