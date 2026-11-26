FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar
ENV SPRING_DOCKER_COMPOSE_ENABLED=false
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
