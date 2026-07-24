# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S ledger && adduser -S ledger -G ledger
COPY --from=build /app/target/ledger-*.jar app.jar
USER ledger
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
