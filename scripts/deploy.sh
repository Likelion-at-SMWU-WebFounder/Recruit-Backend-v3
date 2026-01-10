#!/bin/bash

# EC2 배포 스크립트
APP_NAME="webfounder"
JAR_NAME="webfounder-0.0.1-SNAPSHOT.jar"
APP_PATH="/home/ubuntu/app"
LOG_PATH="/home/ubuntu/logs"

echo "========================================="
echo "배포 시작: $(date)"
echo "========================================="

# JAR 파일 존재 확인
if [ ! -f "$APP_PATH/$JAR_NAME" ]; then
    echo "❌ JAR 파일이 존재하지 않습니다: $APP_PATH/$JAR_NAME"
    exit 1
fi

# 기존 프로세스 확인 및 종료
PID=$(pgrep -f "$JAR_NAME")
if [ ! -z "$PID" ]; then
    echo "기존 애플리케이션 종료 중... (PID: $PID)"
    kill -15 $PID
    
    # Graceful shutdown 대기 (최대 30초)
    for i in {1..30}; do
        if ! pgrep -f "$JAR_NAME" > /dev/null; then
            echo "애플리케이션이 정상적으로 종료되었습니다"
            break
        fi
        echo "종료 대기 중... ($i/30)"
        sleep 1
    done
    
    # 여전히 실행 중이면 강제 종료
    if pgrep -f "$JAR_NAME" > /dev/null; then
        echo "강제 종료 실행..."
        pkill -9 -f "$JAR_NAME"
        sleep 2
    fi
fi

# 환경변수 파일 존재 확인
ENV_FILE="/home/ubuntu/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo "⚠️  환경변수 파일이 없습니다: $ENV_FILE"
    echo "환경변수를 시스템 환경변수로 설정해야 합니다."
fi

# JAR 파일 실행 권한 부여
chmod +x "$APP_PATH/$JAR_NAME"

# 로그 디렉토리 생성
mkdir -p "$LOG_PATH"

echo "애플리케이션 시작 중..."

# 애플리케이션 시작 (환경변수 포함)
cd "$APP_PATH"

# 환경변수 파일이 있으면 로드
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
fi

# Spring Boot 애플리케이션 시작
nohup java -jar \
    -Dspring.profiles.active=prod \
    -Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Dserver.port=8080 \
    -Dlogging.file.path="$LOG_PATH" \
    "$JAR_NAME" > "$LOG_PATH/app.log" 2>&1 &

NEW_PID=$!
echo "새 애플리케이션이 시작되었습니다 (PID: $NEW_PID)"

# 애플리케이션 시작 대기
echo "애플리케이션 초기화 대기 중..."
sleep 20

# 프로세스 실행 확인
if ps -p $NEW_PID > /dev/null; then
    echo "✅ 프로세스가 실행 중입니다"
else
    echo "❌ 프로세스가 실행되지 않습니다"
    echo "최근 로그:"
    tail -50 "$LOG_PATH/app.log"
    exit 1
fi

echo "========================================="
echo "배포 완료: $(date)"
echo "PID: $NEW_PID"
echo "로그 확인: tail -f $LOG_PATH/app.log"
echo "상태 확인: curl http://localhost:8080/actuator/health"
echo "========================================="