# The website's image: build the WAR, then run it on Open Liberty with Java 25.
#
# Stage 1 builds only web-ui (and the parent pom it inherits from).
# Stage 2 is IBM's official Open Liberty image in its "kernel-slim" form:
# features.sh downloads just the features server.xml lists, so the image
# carries only what this app uses. Liberty runs as a non-root user (1001).

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -DskipTests -pl web-ui -am package

FROM icr.io/appcafe/open-liberty:26.0.0.10-kernel-slim-java25-openj9-ubi-minimal
COPY --chown=1001:0 web-ui/src/main/liberty/config/server.xml /config/
RUN features.sh
COPY --chown=1001:0 --from=build /src/web-ui/target/web-ui.war /config/apps/
RUN configure.sh
