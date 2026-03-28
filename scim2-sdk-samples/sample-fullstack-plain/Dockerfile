FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the entire SDK repo — needed to resolve parent pom and sibling modules
COPY . .

# Build only this sample module (and its dependencies)
RUN mvn -B install -DskipTests -pl scim2-sdk-samples/sample-fullstack-plain -am

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/scim2-sdk-samples/sample-fullstack-plain/target/sample-fullstack-plain-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
