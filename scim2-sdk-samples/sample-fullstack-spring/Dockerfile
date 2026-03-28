FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Create a standalone pom for Docker build (doesn't need SDK parent)
COPY docker/docker-pom.xml pom.xml
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
