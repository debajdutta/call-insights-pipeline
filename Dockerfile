# syntax=docker/dockerfile:1
#
# One image definition for all six Spring Boot services. Pick the module with a build arg:
#   docker build --build-arg SERVICE=gateway-service .
# The build context is the repo root, because a module needs the root pom.xml and
# callinsights-common to build. docker-compose.yml passes SERVICE for each service.
# No secrets: nothing here takes an API key; it is passed at run time by compose.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build

# The reactor needs every declared module's pom to resolve, even when building one module.
COPY pom.xml ./
COPY callinsights-common/pom.xml callinsights-common/
COPY call-generator/pom.xml call-generator/
COPY transcription-service/pom.xml transcription-service/
COPY summary-service/pom.xml summary-service/
COPY evaluation-service/pom.xml evaluation-service/
COPY catalog-service/pom.xml catalog-service/
COPY gateway-service/pom.xml gateway-service/

# Sources: the shared module and the selected service only. ${SERVICE:?...} makes a build
# without SERVICE fail fast, before any step runs, instead of building the whole reactor.
COPY callinsights-common/src callinsights-common/src
COPY ${SERVICE:?set-build-arg-SERVICE-for-example-gateway-service}/src ${SERVICE}/src

# Same reactor command as scripts/services.sh. Tests are not run here; mvn -q verify and CI
# are the test gate. The fixed cache id lets all six service builds share one Maven
# repository; "locked" keeps parallel builds from writing to it at the same time.
RUN --mount=type=cache,id=maven-repo,target=/root/.m2,sharing=locked \
    mvn -B -q -DskipTests -pl "${SERVICE}" -am package

FROM eclipse-temurin:21-jre
# Build args do not cross stages, so SERVICE is declared again.
ARG SERVICE
# Non-root runtime user. /data/media-store is the mount point of the shared media-data volume
# (a fresh named volume inherits this ownership); /app/logs is where Logback writes
# logs/<service>.log relative to the working directory.
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app/logs /data/media-store \
    && chown app:app /app/logs /data/media-store
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
ENTRYPOINT ["java","-jar","/app/app.jar"]
