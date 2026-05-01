# ── BUILD STAGE ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first so Maven can cache dependencies separately
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── RUN STAGE ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/music-rhythm-match-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]