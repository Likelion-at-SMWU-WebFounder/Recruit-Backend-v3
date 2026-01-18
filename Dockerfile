# Dockerfile
FROM eclipse-temurin:11-jre

WORKDIR /app

ARG JAR_FILE=build/libs/webfounder-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app/webfounder.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/webfounder.jar"]