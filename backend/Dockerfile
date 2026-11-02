# Stage 1: jar banao
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Stage 2: sirf chalane wala chhota image
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 sentinel
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER sentinel
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -jar app.jar"]