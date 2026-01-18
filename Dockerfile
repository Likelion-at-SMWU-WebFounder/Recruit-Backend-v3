# Dockerfile
FROM eclipse-temurin:11-jre

ARG JAR_FILE=build/libs/webfounder-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /webfounder.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/webfounder.jar"]