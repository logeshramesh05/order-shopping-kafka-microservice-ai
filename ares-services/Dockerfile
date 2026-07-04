FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS=""

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
