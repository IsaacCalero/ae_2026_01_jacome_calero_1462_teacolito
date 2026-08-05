# Placeholder — el microservicio `users` todavía no tiene código fuente.
# Mismo patrón multi-stage que teacolito/Dockerfile (Kotlin + Spring Boot + Gradle).
# Reemplazar cuando exista el código real del microservicio.

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN sh ./gradlew bootJar --no-daemon

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
