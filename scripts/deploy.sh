#!/bin/bash
REPO_URL="https://$GITHUB_TOKEN@github.com/Likelion-at-SMWU-WebFounder/Recruit-Backend-v3.git"
APP_DIR="/home/ubuntu/app-source"
JAR_NAME="webfounder-0.0.1-SNAPSHOT.jar"
APP_PATH="/home/ubuntu/app"
LOG_PATH="/home/ubuntu/logs"

echo "Git pull 배포 시작: $(date)"

# 기존 프로세스 종료
sudo pkill -f "$JAR_NAME" || true
sleep 5

# Git clone 또는 pull
if [ ! -d "$APP_DIR" ]; then
  git clone $REPO_URL $APP_DIR
else
  cd $APP_DIR
  git pull origin main
fi

cd $APP_DIR
chmod +x ./gradlew
./gradlew clean build -x test

# JAR 복사 및 실행
cp build/libs/$JAR_NAME $APP_PATH/
cd $APP_PATH

source /home/ubuntu/.env
nohup java -jar -Dspring.profiles.active=prod $JAR_NAME > $LOG_PATH/app.log 2>&1 &

echo "배포 완료: $(date)"
