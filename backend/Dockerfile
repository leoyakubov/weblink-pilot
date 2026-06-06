FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY . .
RUN mvn -pl application -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/application/target/application-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
