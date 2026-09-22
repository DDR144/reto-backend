# ---- Etapa de build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias (capa estable).
COPY pom.xml .
RUN mvn -B -q dependency:resolve-plugins dependency:resolve || true

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa de runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
