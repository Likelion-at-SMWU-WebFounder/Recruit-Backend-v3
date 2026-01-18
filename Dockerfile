FROM openjdk:11-jre-slim

# 필요한 패키지 설치 (curl for health check)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# JAR 파일 복사
COPY webfounder-0.0.1-SNAPSHOT.jar app.jar

# 로그 디렉터리 생성
RUN mkdir -p /app/logs

# 포트 노출
EXPOSE 8080

# 메모리 최적화된 JVM 옵션으로 애플리케이션 실행
CMD ["java", "-jar", \
     "-Xms256m", \
     "-Xmx512m", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-Dspring.profiles.active=prod", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "app.jar"]
