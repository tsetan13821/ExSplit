# syntax=docker/dockerfile:1

# ---- Build stage: full JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so this layer is cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI (they need Docker for Testcontainers), not in the image build.
RUN ./mvnw -B -q -DskipTests package \
 && cp target/*.jar app.jar

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
# Size the heap from the container's memory limit rather than the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
