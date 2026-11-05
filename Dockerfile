FROM docker.io/library/maven:3.9-eclipse-temurin-11@sha256:7bb84a451705605a47d24eb844d4d9ab03e33e131a507db76aafe25db3907f64 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM docker.io/library/eclipse-temurin:11-jre-jammy@sha256:ee9a1cf7847cb4b1f8176f4e96167f89a1148497500b75032575cb43dc69b42d
WORKDIR /usr/app
COPY --from=build /src/target/*.jar app.jar
USER 10001
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
