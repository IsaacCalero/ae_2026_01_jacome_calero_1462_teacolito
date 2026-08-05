# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN sh ./gradlew bootJar --no-daemon

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
