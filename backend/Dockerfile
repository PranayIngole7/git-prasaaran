FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S -G app app

WORKDIR /app

COPY --from=build /workspace/target/git-prasaaran-backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
