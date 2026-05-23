FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .
RUN mvn -pl app -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/app/target/app-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
