# One Dockerfile for every service: pick the service with --build-arg MODULE=booking-service.
#
# Stage 1 builds all modules once (Docker caches it, so the other services reuse it).
# Stage 2 copies only that service's jar into a small Java runtime image.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:25-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0-SNAPSHOT.jar app.jar

# never run as root inside the container
RUN useradd --system --uid 10001 easytravel
USER easytravel

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
