# syntax=docker/dockerfile:1

FROM gradle:jdk21-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN gradle :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S leadbot && adduser -S leadbot -G leadbot

WORKDIR /app
COPY --from=build /workspace/app/build/libs/leadbot.jar /app/leadbot.jar

USER leadbot

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/leadbot.jar"]
