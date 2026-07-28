FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/target/funcionario-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
