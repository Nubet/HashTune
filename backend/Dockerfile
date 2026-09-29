FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY backend/ ./backend/
WORKDIR /workspace/backend
RUN sh ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install --no-install-recommends -y ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/backend/build/libs/*.jar app.jar
COPY data/dev/mtg_jamendo_mini /app/data/dev/mtg_jamendo_mini

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
