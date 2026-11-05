FROM docker.io/library/maven:3.9-eclipse-temurin-11@sha256:7bb84a451705605a47d24eb844d4d9ab03e33e131a507db76aafe25db3907f64 AS test
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY payloads ./payloads
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn -B verify

FROM docker.io/library/eclipse-temurin:11-jre-jammy@sha256:ee9a1cf7847cb4b1f8176f4e96167f89a1148497500b75032575cb43dc69b42d
WORKDIR /app
COPY --from=test --chown=10001:10001 /app/target/onlinebankingrestapi-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=35.0", "-XX:MaxMetaspaceSize=96m", "-XX:ReservedCodeCacheSize=24m", "-XX:+UseSerialGC", "-Xss512k", "-XX:-UsePerfData", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]